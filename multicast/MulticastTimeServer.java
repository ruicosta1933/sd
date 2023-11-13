import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.Map;

public class MulticastTimeServer {

    private static final String MULTICAST_GROUP = "224.0.0.1";
    private static final int MULTICAST_PORT = 5005;
    private static final int BUFFER_SIZE = 1024;
    private static final int NUM_DATAGRAMS = 10; // Número de datagramas a enviar

    public static void main(String[] args) {
        try {
            // Inicia o servidor
            startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startServer() throws IOException {
        InetAddress group = InetAddress.getByName(MULTICAST_GROUP);

        // Thread para lidar com os pedidos dos clientes
        Thread serverThread = new Thread(() -> {
            try (MulticastSocket socket = new MulticastSocket(MULTICAST_PORT)) {
                socket.joinGroup(group);

                // Mapa para armazenar o tempo de envio de cada datagrama
                Map<Integer, Long> sendTimes = new HashMap<>();

                for (int i = 0; i < NUM_DATAGRAMS; i++) {
                    // Envia o datagrama com um número de sequência
                    long currentTime = System.currentTimeMillis();
                    sendTimeRequest(socket, group, currentTime, i);

                    // Armazena o tempo de envio
                    sendTimes.put(i, currentTime);
                }

                // Aguarda as respostas e calcula o atraso para cada datagrama
                receiveTimeResponses(socket, sendTimes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Thread para atuar como cliente interno do servidor
        Thread clientThread = new Thread(() -> {
            try {
                // Inicia o cliente interno
                startInternalClient();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Inicia as threads
        serverThread.start();
        clientThread.start();

        try {
            // Aguarda até que ambas as threads terminem
            serverThread.join();
            clientThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void sendTimeRequest(MulticastSocket socket, InetAddress group, long currentTime, int sequenceNumber) throws IOException {
        byte[] timeRequest = Long.toString(currentTime).getBytes();
        byte[] sequenceNumberBytes = Integer.toString(sequenceNumber).getBytes();

        // Adiciona o número de sequência aos dados
        byte[] sendData = new byte[timeRequest.length + sequenceNumberBytes.length + 1];
        System.arraycopy(timeRequest, 0, sendData, 0, timeRequest.length);
        sendData[timeRequest.length] = (byte) '|';
        System.arraycopy(sequenceNumberBytes, 0, sendData, timeRequest.length + 1, sequenceNumberBytes.length);

        DatagramPacket packet = new DatagramPacket(sendData, sendData.length, group, MULTICAST_PORT);
        socket.send(packet);
    }

    private static void receiveTimeResponses(MulticastSocket socket, Map<Integer, Long> sendTimes) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        // Mapa para armazenar o tempo de recebimento de cada resposta
        Map<Integer, Long> receiveTimes = new HashMap<>();

        for (int i = 0; i < NUM_DATAGRAMS; i++) {
            // Aguarda a resposta
            socket.receive(packet);

            // Processa a resposta
            String[] response = new String(packet.getData(), 0, packet.getLength()).split("\\|");
            long receiveTime = System.currentTimeMillis();
            int sequenceNumber = Integer.parseInt(response[1]);

            // Armazena o tempo de recebimento
            receiveTimes.put(sequenceNumber, receiveTime);
        }

        // Calcula o atraso para cada datagrama
        for (int i = 0; i < NUM_DATAGRAMS; i++) {
            long sendTime = sendTimes.get(i);
            long receiveTime = receiveTimes.get(i);
            long delay = receiveTime - sendTime;

            // Imprime o atraso para cada datagrama
            System.out.println("Atraso para Datagrama " + i + ": " + delay + "ms");
        }
    }

    private static void startInternalClient() throws IOException {
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getLocalHost();

            for (int i = 0; i < NUM_DATAGRAMS; i++) {
                // Solicita o tempo ao próprio servidor
                requestTime(clientSocket, serverAddress);
                try {
                    // Aguarda um curto período entre solicitações
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void requestTime(DatagramSocket socket, InetAddress serverAddress) throws IOException {
        byte[] requestData = "InternalTimeRequest".getBytes();
        DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, serverAddress, MULTICAST_PORT);
        socket.send(requestPacket);
    }
}
