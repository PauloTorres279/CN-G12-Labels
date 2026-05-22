import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.gson.Gson;
import com.google.pubsub.v1.PubsubMessage;

public class LabelsMessageReceiver implements MessageReceiver {
    private final Gson gson = new Gson();
    private final ImageProcessor processor = new ImageProcessor();

    public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer){
        try{
            String json = message.getData().toStringUtf8();

            System.out.println("Mensagem recebida: ");
            System.out.println(json);

            ProcessingRequest request = gson.fromJson(json, ProcessingRequest.class);

            processor.processImage(request);

            consumer.ack();

            System.out.println("Mensagem processada");
        }
        catch (Exception e){
            System.out.println("Erro: " + e.getMessage());
            consumer.nack();
        }
    }
}
