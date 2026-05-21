package labelsServer;

import cn.labels.contract.*;
import io.grpc.stub.StreamObserver;

public class elasticityServiceImplementation extends ElasticityServiceGrpc.ElasticityServiceImplBase {

    @Override
    public void changeInstances(resizeRequest request,
                                StreamObserver<resizeReply> responseObserver) {

        resizeReply reply = resizeReply.newBuilder()
                .setSuccess(false)
                .setMessage("changeInstances ainda não implementado")
                .build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void changeInstancesForImages(resizeRequest request,
                                         StreamObserver<resizeReply> responseObserver) {

        resizeReply reply = resizeReply.newBuilder()
                .setSuccess(false)
                .setMessage("changeInstancesForImages ainda não implementado")
                .build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}