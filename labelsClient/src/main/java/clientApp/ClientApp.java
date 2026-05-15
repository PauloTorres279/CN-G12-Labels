package clientApp;

import cn.labels.contract.ElasticityReplyGrpc;
import cn.labels.contract.LabelsServiceGrpc;
import com.google.protobuf.compiler.PluginProtos;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class ClientApp {

    private static String svcIP = "localhost";
    private static int svcPort = 8000;

    private static LabelsServiceGrpc.LabelsServiceBlockingStub blockingStub;
    private static LabelsServiceGrpc.LabelsServiceStub noBlockStub;

    static void main(String[] args) {
        try {
            if (args.length == 2) {
                svcIP = args[0];
                svcPort = Integer.parseInt(args[1]);
            }

            System.out.println("connect to " + svcIP + ":" + svcPort);

            ManagedChannel channel = ManagedChannelBuilder.forAddress(svcIP, svcPort)
                    .usePlaintext()
                    .build();

            blockingStub = LabelsServiceGrpc.newBlockingStub(channel);
            noBlockStub = LabelsServiceGrpc.newStub(channel);

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
                            unsubscribe(username); break;
                        case 3 :
                            publish(username); break;
                        case 4 :
                            listTopics(); break;
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


        }
    }
}
