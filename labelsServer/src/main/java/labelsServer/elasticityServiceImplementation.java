package labelsServer;

import cn.labels.contract.ElasticityServiceGrpc;
import cn.labels.contract.resizeReply;
import cn.labels.contract.resizeRequest;
import com.google.cloud.compute.v1.InstanceGroupManagersClient;
import com.google.cloud.compute.v1.Operation;
import com.google.cloud.compute.v1.ResizeInstanceGroupManagerRequest;
import io.grpc.stub.StreamObserver;

public class elasticityServiceImplementation extends ElasticityServiceGrpc.ElasticityServiceImplBase {

    @Override
    public void changeInstances(
            resizeRequest request,
            StreamObserver<resizeReply> responseObserver
    ) {
        try {
            resizeManagedInstanceGroup(
                    request.getProjectId(),
                    request.getZone(),
                    request.getInstanceGroupName(),
                    request.getNewSize()
            );

            resizeReply reply = resizeReply.newBuilder()
                    .setSuccess(true)
                    .setMessage("Número de instâncias alterado para "
                            + request.getNewSize())
                    .build();

            responseObserver.onNext(reply);
            responseObserver.onCompleted();

        } catch (Exception e) {
            resizeReply reply = resizeReply.newBuilder()
                    .setSuccess(false)
                    .setMessage("Erro ao alterar número de instâncias: " + e.getMessage())
                    .build();

            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void changeInstancesForImages(
            resizeRequest request,
            StreamObserver<resizeReply> responseObserver
    ) {
        try {
            resizeManagedInstanceGroup(
                    request.getProjectId(),
                    request.getZone(),
                    request.getInstanceGroupName(),
                    request.getNewSize()
            );

            resizeReply reply = resizeReply.newBuilder()
                    .setSuccess(true)
                    .setMessage("Número de instâncias da Labels App alterado para "
                            + request.getNewSize())
                    .build();

            responseObserver.onNext(reply);
            responseObserver.onCompleted();

        } catch (Exception e) {
            resizeReply reply = resizeReply.newBuilder()
                    .setSuccess(false)
                    .setMessage("Erro ao alterar instâncias da Labels App: " + e.getMessage())
                    .build();

            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }

    private void resizeManagedInstanceGroup(
            String projectId,
            String zone,
            String instanceGroupName,
            int newSize
    ) throws Exception {

        try (InstanceGroupManagersClient client = InstanceGroupManagersClient.create()) {

            ResizeInstanceGroupManagerRequest request =
                    ResizeInstanceGroupManagerRequest.newBuilder()
                            .setProject(projectId)
                            .setZone(zone)
                            .setInstanceGroupManager(instanceGroupName)
                            .setSize(newSize)
                            .build();

            Operation operation = client.resizeAsync(request).get();

            System.out.println("Operation ID: " + operation.getName());
            System.out.println("Instance Group: " + instanceGroupName);
            System.out.println("Novo tamanho: " + newSize);
        }
    }
}