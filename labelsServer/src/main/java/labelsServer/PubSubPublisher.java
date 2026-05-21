package labelsServer;

import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;

public class PubSubPublisher {

    public static void pubSubRequest(
        String projectId,
        String topicId,
        String requestId,
        String bucketName,
        String blobName
        ) throws Exception{

        TopicName topicName = TopicName.ofProjectTopicName(projectId, topicId); //nome do tópico Pub/Sub
        Publisher publisher = Publisher.newBuilder(topicName).build();

        String jsonData = """
                {
                "requestId": "%s",
                "bucketName": "%s",
                "blobName": "%s"
                }
                """.formatted(requestId, bucketName, blobName);

        ByteString msgData = ByteString.copyFromUtf8(jsonData); //transformar a mensagem JSON em Bytes porque é o formato de dados aceite pelo Pub/Sub

        PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                .setData(msgData)
                .build();
        ApiFuture<String> future = publisher.publish(pubsubMessage);
        String msgID = future.get();
        System.out.println("Message Published with ID=" + msgID);
        publisher.shutdown();
    }


}
