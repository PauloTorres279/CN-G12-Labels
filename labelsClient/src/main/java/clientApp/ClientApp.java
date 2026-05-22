package clientApp;

import cn.labels.contract.*;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class ClientApp {

    private static String svcIP = "localhost";
    private static int svcPort = 8000;

    private static LabelsServiceGrpc.LabelsServiceBlockingStub blockingStub;
    private static LabelsServiceGrpc.LabelsServiceStub noBlockStub; //usar para chamadas em que o cliente não fica à espera da resposta imediatamente

    private static ElasticityServiceGrpc.ElasticityServiceBlockingStub elasticityBlockingStub;

    public static void main(String[] args) {
        try {
            if (args.length == 2) {
                svcIP = args[0];
                svcPort = Integer.parseInt(args[1]);
            }

            Scanner ipScanner = new Scanner(System.in);

            try{
                System.out.println("Processo de obtenção dos IP's das VM's gRPC");
                List<String> ips = IpLookupFunctionClass.getExternalIps();

                if(ips.isEmpty())
                    System.out.println("Nenhum ip encontrado, a usar: " + svcIP);
                else{
                    System.out.println("Lista de servidores disponíveis: ");
                    for (int i = 0; i < ips.size();i++){
                        System.out.println((i+1) + "-" + ips.get(i));
                    }

                    System.out.println("Escolha o servidor: ");
                    int option = Integer.parseInt(ipScanner.nextLine());

                    svcIP = ips.get(option-1);
                }
            } catch (Exception e) {
                System.out.println("Erro na inicialização do serviço de IP Lookup");
                System.out.println("A utilizar: " + svcIP);
            }

            ManagedChannel channel = ManagedChannelBuilder.forAddress(svcIP, svcPort)
                    .usePlaintext()
                    .build();

            blockingStub = LabelsServiceGrpc.newBlockingStub(channel);
            noBlockStub = LabelsServiceGrpc.newStub(channel);

            elasticityBlockingStub = ElasticityServiceGrpc.newBlockingStub(channel);
            /*Scanner sc = new Scanner(System.in); //usar scanner para obter o username a ser registado
            System.out.print("Username: ");
            String username = sc.nextLine();*/

            while (true) {
                try {
                    int option = menu();
                    Scanner scanner = new Scanner(System.in);
                    switch (option) {
                        case 1 :
                            submitImage(scanner); break;
                        case 2 :
                            imageInfo(scanner); break;
                        case 3 :
                            namesByDate(scanner); break;
                        case 4 :
                            changeInstances(scanner); break;
                        case 5:
                            changeInstancesForImages(scanner); break;
                        case 99 :
                            channel.shutdown();
                            System.exit(0);

                    }
                } catch (Exception ex) {
                    System.out.println("Execution call error!");
                    ex.printStackTrace();
                }
            }

        } catch (Exception ex) {
            System.out.println("Unhandled exception");
            ex.printStackTrace();
        }
    }

    private static int menu() {
        int op;
        Scanner scan = new Scanner(System.in);
        do {
            System.out.println();
            System.out.println("    MENU");
            System.out.println(" 1 - Submissão de imagem");
            System.out.println(" 2 - Listar características encontradas numa imagem");
            System.out.println(" 3 - Obter imagens por data e label");
            System.out.println(" 4 - Ajustar número de instâncias do servidor gRPC");
            System.out.println(" 5 - Ajustar número de instâncias da aplicação de processamento de imagens");
            System.out.println("99 - Exit");
            System.out.println();
            System.out.println("Choose an Option?");
            op = scan.nextInt();
        } while (!((op >= 1 && op <= 5) || op == 99));
        return op;
    }

    private static void submitImage(Scanner sc){
        try{
            System.out.println("Caminho da imagem");
            String imagePath = sc.nextLine();

            Path path = Path.of(imagePath);

            if(!Files.exists(path))
                System.out.println("Ficheiro não encontrado!");

            StreamObserver<imageReply> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(imageReply reply) {
                    System.out.println("Bloco submetido com sucesso.");
                    System.out.println("ID do pedido: " + reply.getRequestId());
                    System.out.println("Mensagem: " + reply.getMessage());
                }

                @Override
                public void onError(Throwable t) {
                    System.out.println("Erro na submissão da imagem: " + t.getMessage());
                }

                @Override
                public void onCompleted() {
                    System.out.println("Envio terminado");
                }
            }; //definição do que fazer quando o servidor me responder

            //criação de um stream para se poder enviar blocos de imagem. Com o responseObserver já se sabe o que fazer quando o servidor responder
            StreamObserver<imageBlock> requestObserver = noBlockStub.submitImage(responseObserver);

            byte[] allImageBytes = Files.readAllBytes(path); //transformar a imagem num array de bytes
            int blockSize = 64*1024; //blocos de 64KB
            String fileName = path.getFileName().toString();
            String contentType = Files.probeContentType(path);

            for(int actSize = 0; actSize < allImageBytes.length; actSize += blockSize){
                int length = Math.min(blockSize, allImageBytes.length - actSize); //caso o último bloco tenha menos de 64KB

                imageBlock block = imageBlock.newBuilder()
                        .setFileName(fileName)
                        .setData(ByteString.copyFrom(allImageBytes, actSize, length))//copiar X bytes para o imageBlock
                        .setContentType(contentType)
                        .build();
                requestObserver.onNext(block);
            }

            requestObserver.onCompleted();

        }
        catch (Exception e) {
            System.out.println("Erro no envio da imagem: " + e.getMessage());
        }

    }

    private static void imageInfo(Scanner sc){
        try{
            System.out.println("ID do pedido: ");
            String requestID = sc.nextLine();

            imageInfoRequest request = imageInfoRequest.newBuilder() //definir a mensagem a ser enviada ao servidor, com o requestId
                    .setRequestId(requestID)
                    .build();

            imageInfoReply reply = blockingStub.imageInfo(request); //obter a resposta desejada

            System.out.println("Informações acerca da imagem recebida: ");
            System.out.println("ID do pedido: " + reply.getRequestId());
            System.out.println("Nome do ficheiro: " + reply.getFileName());
            System.out.println("Nome do bucket: " + reply.getBucketName());
            System.out.println("Nome do blob: " + reply.getBlobName());
            System.out.println("Data de processamento da imagem: " + reply.getProcessedAt());

            if(!reply.getErrorMessage().isEmpty())
                System.out.println("Erro: " + reply.getErrorMessage());

            System.out.println("Labels: ");
            for(LabelInfo label : reply.getLabelsList()){
                System.out.println("Label em inglês: " + label.getLabelEn() + " - Label em português: " + label.getLabelPt() + " - Nível de confiança: " + label.getConfidence());
            }
        }
        catch (Exception e){
            System.out.println("Erro na obtenção de informações: " + e.getMessage());
        }
    }

    private static void namesByDate(Scanner sc){
        try{
            System.out.println("Label a pesquisar: ");
            String label = sc.nextLine();

            System.out.println("Data de início: ");
            String startDate = sc.nextLine();

            System.out.println("Data de fim: ");
            String endDate = sc.nextLine();

            namesByDateRequest request = namesByDateRequest.newBuilder()
                    .setLabel(label)
                    .setStartDate(startDate)
                    .setEndDate(endDate)
                    .build();

            namesByDateReply reply = blockingStub.namesByDate(request);

            System.out.println("Ficheiros armazenados no sistema:");
            for(ImageSummary is : reply.getImagesList()){
                System.out.println("ID do pedido: " + is.getRequestId());
                System.out.println("Nome do ficheiro: " + is.getFileName());
                System.out.println("Nome do bucket: " + is.getBucketName());
                System.out.println("Nome do blob: " + is.getBlobName());
                System.out.println("Processado em: "+ is.getProcessedAt());

                System.out.println("Labels: ");
                for(LabelInfo li : is.getLabelsList()){
                    System.out.println("Label em inglês: " + li.getLabelEn() + " - Label em português: " + li.getLabelPt() + " - Nível de confiança: " + li.getConfidence());
                }
            }

        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    private static void changeInstances(Scanner sc){
        try{
            resizeReply reply = elasticityBlockingStub.changeInstances(scanInfo(sc));

            System.out.println("Resultado: " + reply.getSuccess());
            System.out.println("Mensagem: " + reply.getMessage());

        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
        }
    }

    private static void changeInstancesForImages(Scanner sc){
        try{
            resizeReply reply = elasticityBlockingStub.changeInstancesForImages(scanInfo(sc));

            System.out.println("Resultado: " + reply.getSuccess());
            System.out.println("Mensagem: " + reply.getMessage());
        }
        catch(Exception e){
            System.out.println("Erro: " + e.getMessage());
        }
    }

    private static resizeRequest scanInfo(Scanner sc){
        System.out.println("ID do projeto: ");
        String projectId = sc.nextLine();

        System.out.println("Zona: ");
        String zone = sc.nextLine();

        System.out.println("Nome do group instance: ");
        String instanceGroupName = sc.nextLine();

        System.out.println("Novo tamanho: ");
        int newSize = Integer.parseInt(sc.nextLine());

        return resizeRequest.newBuilder()
                .setProjectId(projectId)
                .setZone(zone)
                .setInstanceGroupName(instanceGroupName)
                .setNewSize(newSize)
                .build();
    }

}
