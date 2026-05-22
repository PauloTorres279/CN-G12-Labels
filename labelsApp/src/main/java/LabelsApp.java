import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;

public class LabelsApp {

    private static final String PROJECT_ID = "cn2526-t3-g12";
    private static final String SUBSCRIPTION_ID = "labels-g12-topic-sub";

    public static void main(String[] args){
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(PROJECT_ID, SUBSCRIPTION_ID);

        LabelsMessageReceiver receiver = new LabelsMessageReceiver();
        Subscriber subscriber = null;

        try{
            subscriber = Subscriber.newBuilder(subscriptionName, receiver).build();
            subscriber.startAsync().awaitRunning();

            System.out.println("LabelsApp iniciada");
            System.out.println("Subscription: " + SUBSCRIPTION_ID);

            subscriber.awaitTerminated();
        }
        catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
            if(subscriber != null)
                subscriber.stopAsync();
        }
        }
    }
}
