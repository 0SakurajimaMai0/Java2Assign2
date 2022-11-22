package application.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Controller implements Initializable {
    private static final int PLAY_1 = 1;
    private static final int PLAY_2 = 2;
    private static final int EMPTY = 0;
    private static final int BOUND = 90;
    private static final int OFFSET = 15;

    @FXML
    private Pane base_square;

    @FXML
    private Rectangle game_panel;

    public Text getEnd() {
        return end;
    }

    @FXML
    private Text end =new Text();

    public Text getDisconnect() {
        return disconnect;
    }

    @FXML
    private Text disconnect = new Text();

    private static boolean TURN = false;

    private static final int[][] chessBoard = new int[3][3];
    private static final boolean[][] flag = new boolean[3][3];

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        Server server = new Server();
        server.init();
        Client client = new Client();
        client.init();

    }

    private void drawChess () {
        for (int i = 0; i < chessBoard.length; i++) {
            for (int j = 0; j < chessBoard[0].length; j++) {
                if (flag[i][j]) {
                    // This square has been drawing, ignore.
                    continue;
                }
                switch (chessBoard[i][j]) {
                    case PLAY_1:
                        drawCircle(i, j);
                        break;
                    case PLAY_2:
                        drawLine(i, j);
                        break;
                    case EMPTY:
                        // do nothing
                        break;
                    default:
                        System.err.println("Invalid value!");
                }
            }
        }
    }

    private void drawCircle (int i, int j) {
        Circle circle = new Circle();
        base_square.getChildren().add(circle);
        circle.setCenterX(i * BOUND + BOUND / 2.0 + OFFSET);
        circle.setCenterY(j * BOUND + BOUND / 2.0 + OFFSET);
        circle.setRadius(BOUND / 2.0 - OFFSET / 2.0);
        circle.setStroke(Color.RED);
        circle.setFill(Color.TRANSPARENT);
        flag[i][j] = true;
    }

    private void drawLine (int i, int j) {
        Line line_a = new Line();
        Line line_b = new Line();
        base_square.getChildren().add(line_a);
        base_square.getChildren().add(line_b);
        line_a.setStartX(i * BOUND + OFFSET * 1.5);
        line_a.setStartY(j * BOUND + OFFSET * 1.5);
        line_a.setEndX((i + 1) * BOUND + OFFSET * 0.5);
        line_a.setEndY((j + 1) * BOUND + OFFSET * 0.5);
        line_a.setStroke(Color.BLUE);

        line_b.setStartX((i + 1) * BOUND + OFFSET * 0.5);
        line_b.setStartY(j * BOUND + OFFSET * 1.5);
        line_b.setEndX(i * BOUND + OFFSET * 1.5);
        line_b.setEndY((j + 1) * BOUND + OFFSET * 0.5);
        line_b.setStroke(Color.BLUE);
        flag[i][j] = true;
    }
    public boolean Judge(int PlayerNumber){
        boolean endgame = false;

        for(int i=0;i<3;i++){
            int count = 0 ;
            for(int j=0;j<3;j++){
                if(chessBoard[i][j]== PlayerNumber){
                    count++;
                }
            }
            if(count==3){
                endgame=true;
                break;
            }
        }

        for(int i=0;i<3;i++){
            int count = 0 ;
            for(int j=0;j<3;j++){
                if(chessBoard[j][i]== PlayerNumber){
                    count++;
                }
            }
            if(count==3){
                endgame=true;
                break;
            }
        }

        if(chessBoard[0][0]==PlayerNumber&&chessBoard[1][1]==PlayerNumber&&chessBoard[2][2]==PlayerNumber){
            endgame=true;
        }

        if(chessBoard[2][0]==PlayerNumber&&chessBoard[1][1]==PlayerNumber&&chessBoard[0][2]==PlayerNumber){
            endgame=true;
        }

        return endgame;
    }
    class Client  {
        private Socket socket;
        private DataInputStream dis;
        private PrintStream ps;
        private int PlayerNumber;

        public void init()  {
            try{
                socket = new Socket(InetAddress.getLocalHost(),8888);
                dis = new DataInputStream(socket.getInputStream());
                ps = new PrintStream(socket.getOutputStream());
                Listener listener = new Listener(socket,this);
                listener.start();
                this.PlayerNumber= listener.PlayerNumber;

                game_panel.setOnMouseClicked(e ->{
                    if(PlayerNumber==1&&TURN){
                        int x = (int) (e.getX() / BOUND);
                        int y = (int) (e.getY() / BOUND);
                        chessBoard[x][y]=1;
                        send(x+" "+y);
                        TURN = false;
                        if(Judge(1)){
                            getEnd().setText("你赢了");
                        }
                    }
                    if(PlayerNumber==2&& !TURN){
                        int x = (int) (e.getX() / BOUND);
                        int y = (int) (e.getY() / BOUND);
                        chessBoard[x][y]=2;
                        send(x+" "+y);
                        TURN = true ;
                        if(Judge(2)){
                            getEnd().setText("你赢了");
                        }
                    }
                });



            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        public void send(String s){
            ps.println(s);
            ps.flush();
        }

    }

    class Server  {
        private ServerSocket ss;
        private Socket socket;
        private DataInputStream dis;
        private PrintStream ps;
        private List<Player>players =new ArrayList<>();


        public void init(){
            int port = 8888;
            try{
                ss = new ServerSocket(port);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Link link = new Link();
            link.start();
            Disconnect disconnect = new Disconnect();
            disconnect.start();

        }

        class Link extends Thread{
            public void run() {
                while (true){
                    if (players.size() < 2){
                        try {
                            socket = ss.accept();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Player player = new Player(socket,players.size()+1);
                        synchronized (new Link()) {
                            players.add(player);
                        }
                        player.start();
                    }
                    else {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        class Player extends Thread{
            Socket socket;
            PrintStream ps;
            DataInputStream dis;
            private int PlayerNumber;


            public Player (Socket socket,int pn){
                this.socket=socket;
                this.PlayerNumber=pn;

                try {
                    ps = new PrintStream(socket.getOutputStream());
                    dis =new DataInputStream(socket.getInputStream());
                    send(Integer.toString(pn));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            public void run(){
                while (true){
                    String S = null;
                    try {
                        S = dis.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(S!=null){
                        String[]s =S.split(" ");
                        for (Player player:players){
                            if(PlayerNumber!=player.PlayerNumber){
                                send(S);
                            }
                        }
                    }
                }
            }
            public void send(String s){
                ps.println(s);
                ps.flush();
            }
            public void close(){
                try {
                    dis.close();
                    ps.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }



        }
        class Disconnect extends Thread{
            private Lock PlayerLock= new ReentrantLock();
            private Condition dc = PlayerLock.newCondition();
            public void run(){
                PlayerLock.lock();
                if(players.size()==2){
                    try {
                        dc.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(players.size()==1){
                        dc.signalAll();
                        getDisconnect().setText("对方玩家掉线");
                    }
                }
                PlayerLock.unlock();
            }
        }



    }
    class Listener extends Thread{
        DataInputStream dis;
        PrintStream ps;
        Socket socket;
        Client client;
        int PlayerNumber;

        public Listener(Socket socket,Client client){
            this.socket=socket;
            this.client=client;
            try {
                if(socket!=null){
                    dis = new DataInputStream(socket.getInputStream());
                    ps = new PrintStream(socket.getOutputStream());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){
            String S = null;
            while (true){
                try {
                S=dis.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
                assert S != null;
                String[]s = S.split(" ");
            if(s.length==1){
                this.PlayerNumber=Integer.parseInt(s[0]);
            }
            if(s.length==2){
                int x = Integer.parseInt(s[0]);
                int y = Integer.parseInt(s[1]);
                if(PlayerNumber==1){
                    chessBoard[x][y]=2;
                    drawChess();
                    TURN = true;
                    if(Judge(2)){
                        getEnd().setText("你输了");
                    }
                }
                if(PlayerNumber==2){
                    chessBoard[x][y]=1;
                    drawChess();
                    TURN =false;
                    if(Judge(1)){
                        getEnd().setText("你输了");
                    }

                }

            }
            }
        }
    }
}
