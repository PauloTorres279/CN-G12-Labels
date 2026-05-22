package labelsServer;

import cn.labels.contract.*;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.grpc.stub.StreamObserver;

import java.io.ByteArrayOutputStream;
import java.util.UUID;
import java.util.stream.Stream;

public class labelsServiceImplementation extends LabelsServiceGrpc.LabelsServiceImplBase {

    private static final String PROJECT_ID = "cn2526-t3-g12";
    private static final String BUCKET_NAME = "cn-labels-g12-t3-v2526";
    private static final String TOPIC_ID = "labels-processing-topic";

    StorageOptions storageOptions = StorageOptions.getDefaultInstance();
    Storage storage = storageOptions.getService();


    private final StorageOperations storageOperations = new StorageOperations(storage);
    private final firestoreOperations firestoreOperations = new firestoreOperations();

    public StreamObserver<imageBlock> submitImage (StreamObserver<imageReply> responseObserver){
        return new StreamObserver<imageBlock>() {

            private final ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
            private String fileName = "";
            private String contentType = "";

            @Override
            public void onNext(imageBlock block) {
                try{
                    if(!block.getFileName().isEmpty())
                        fileName = block.getFileName();
                    if(!block.getContentType().isEmpty())
                        contentType = block.getContentType();

                    block.getData().writeTo(imageBytes); //juntar os blocos ao array de bytes

                    System.out.println("Recebido bloco com " + block.getData().size() + "bytes");
                }
                catch (Exception e){
                    responseObserver.onError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("Erro ao receber parte da imagem");
            }

            @Override
            public void onCompleted() { //chega aqui porque o gRPC chama-o quando o cliente termina a stream
                try{
                    String requestId = UUID.randomUUID().toString(); //criar identificador do pedido
                    String blobName = fileName + "-" + requestId;

                    byte[] completeImage = imageBytes.toByteArray();

                    storageOperations.uploadBlobToBucket(BUCKET_NAME, blobName, contentType, completeImage); //armazenar a imagem no cloud storage

                    firestoreOperations.createPendingRequest(requestId, fileName, BUCKET_NAME, blobName);

                    PubSubPublisher.pubSubRequest(PROJECT_ID, TOPIC_ID, requestId, BUCKET_NAME, blobName); //enviar uma mensagem para o topico Pub/Sub para as apps de processamento de imagens

                    imageReply reply = imageReply
                            .newBuilder()
                            .setRequestId(requestId)
                            .setMessage("Imagem submetida com sucesso")
                            .build();

                    responseObserver.onNext(reply);
                    responseObserver.onCompleted();

                }
                catch (Exception e){
                    System.out.println("Erro no onCompleted: " + e.getMessage());
                }
            }
        };
    }

    public void imageInfo(imageInfoRequest request, StreamObserver<imageInfoReply> responseObserver){
        try{
            imageInfoReply reply = firestoreOperations.getImageInfo(request.getRequestId());

            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
        catch (Exception e){
            System.out.println("Erro: " + e.getMessage());
        }
    }

    public void namesByDate(namesByDateRequest request, StreamObserver<namesByDateReply> responseObserver){
        try{
            namesByDateReply reply = firestoreOperations.getNamesByDate(request.getLabel(), request.getStartDate(), request.getEndDate());

            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
        catch (Exception e){
            System.out.println("Erro: " + e.getMessage());
        }
    }
}