package org.academiadecodigo.wizards;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*

------------ COMPLEXIDADE --------------------
- ter que dizer UNO para ganhar;
- anunciar o numero da cartas de cada jogador a cada jogada;
- add: reverse, +2 e block;

 */
public class Server {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    public static int port;
    public static int playerNum;
    private LinkedList<Player> players;
    private ExecutorService fixedPool;
    private Deck deck;
    private LinkedList<Card> discardedPile;
    private Card lastCardPlayed;
    private Card cardPlayed;

    public static void main(String[] args) {
        // Grab port number and max player count from args, else initialize with some defaults
        System.out.println("server is working and the game is running");
        if (args.length != 2) {
            port = 8080;
            playerNum = 2;
        } else {
            port = Integer.parseInt(args[0]);
            playerNum = Integer.parseInt(args[1]);
        }

        Server server = new Server();
        try {
            server.listen();
            server.gameLoop();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Server() {
        try {
            serverSocket = new ServerSocket(port);
            players = new LinkedList<>();
            deck = new Deck();
            discardedPile = new LinkedList<>();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        fixedPool = Executors.newFixedThreadPool(playerNum);
    }

    public void listen() throws IOException {

        String artUno = "\n" +
                "                                      ,╓╗@▒▒╣╣╣╬╬╬╣╣╣▒@╗╓,\n" +
                "                                ,╔@╣╬╬╬╬╬╬╬╬╬╬╬╬╬╬╬╬╬╣╩╙╓▄▄R▀▀W▄▄,\n" +
                "                            ,╗▒╬╬╬╬╬╬╬╬╬╬╬╬╬╬╬╬╬╬╬╬╩└▄▀▀╦φφ░ΓΓ░φφ╦▀▀▄\n" +
                "                         ╓▒╣╬╬╬╬╬╬╬╬╬╬╬╬╬╬╝╜╙╓▄└╬╙,▓▀≤φΓΓ░╙┘▒Γ²╚ΓΓΓφ╥▀▄\n" +
                "                      ╓▒╬╬╬╬╬╬╬╬╬╬╬╬╬╬╣╙╓▄▀▀╫╦╙▄ ▓█▀φΓΓ╚▄███████▌└φΓΓφ╨▌\n" +
                "                    #╣╬╬╬╬╬╬╬╬╝╨╙╝╬╬╬┘▄███▄φΓΓ≥╫██▌φΓΓ╙╫█▀▀▀▀██▓▓█▄╚ΓΓ░╙▌\n" +
                "                 ,@╬╬╬╬╬╣╝╙├▄▄▀▀▀▀▄└╚╕╙█▓▓█∩ΓΓΓε██⌐ΓΓΓ⌐█ ╣╬╬▒┐▀████µΓΓΓε█\n" +
                "                @╬╬╬╜╙▄▄▀▀▀█▌7φΓΓφ≥╫▀▄ ▓████\"ΓΓΓ⌐█▌φΓΓ≥╫⌐╚╬╬╬╬╕╫███▌ΓΓΓ░█\n" +
                "              «╣╬╣╙╓▓█▌7φΓφ╨█▄φΓΓΓΓΓφ≥╠▀█████\\ΓΓΓ└█'ΓΓΓε▀▄╙╣╬╬╣ ███]ΓΓΓ⌐█\n" +
                "          ╓▄Æ▓─╟╬▒\"█▓▓█▄φΓΓφ╫█┘░ΓΓ░\"╚ΓΓΓφ╦▀██▌╙ΓΓφ╙█└░ΓΓφ╚▀╗▄▄▄██▀≤ΓΓΓ░▓\n" +
                "      ,▓█╙#φφ[█ ╣╬ε╙████µ░ΓΓ≥██\"ΓΓΓ[█▌▄7░ΓΓφ≥╫¬╚ΓΓφ╙█▄²ΓΓΓΓφφφφφ░ΓΓΓ╙▄█\n" +
                "     ▀████\\ΓΓΓ⌐█ ╬╬ ╫████\"ΓΓΓ[██\\ΓΓΓ⌐███▓▄└φΓΓ░φΓΓΓ≥▓██▓▄\"═δ░Γ░δ╙²▄▄█▀\n" +
                "      ████▌╙ΓΓ░╙▌└╬╣ █████²ΓΓΓ⌐█▌╙ΓΓφ╙██████▄└╚ΓΓΓΓΓε██████████████╙\n" +
                "       ████▌φΓΓφ╙▌╙╬╣ █████╙ΓΓ░╙█▌╚ΓΓφ╙████████▌▄╙Γ╚²╓█▀██████▀▀└\n" +
                "       ╙██▓█▄░ΓΓ≥▓µ╚╬▒ ████⌐ΓΓΓε██µφΓΓ≥▓⌐ ▀███████████,é▒@@@▒▒┘\n" +
                "        ╫█▓██└ΓΓΓε█▄╙╣L▐███«ΓΓΓ⌐███└ΓΓΓε█ ╟▒╓╙▀██▀▀╠╓@╬╬╬╬╬╬╜\n" +
                "         █████└ΓΓΓφ╩▀▀R▀▀╠φΓΓΓ╙▄████▄▄▓██,#╬╬╬▒╗@╣╬╬╬╬╬╬╬╣╙\n" +
                "          █████▄╙ΓΓΓΓ░░░ΓΓΓΓ╙▄▓└██████▀,#╣╬╬╬╬╬╬╬╬╬╬╬╬╬╝└\n" +
                "           ▀█████▓▄░═╙╙²Q▄▄██└╔▒ ╟╓╔@▒╣╬╬╬╬╬╬╬╬╬╬╬╬╬╝╙\n" +
                "            └█████████████▀╓#╣╬╬╬╬╬╬╬╬╬╬╬╬╬╬╬╬╬╬╣╨└\n" +
                "               └▀▀▀▀▀▀▀▄╔@╣╬╬╬╬╬╬╬╬╬╬╬╬╬╬╬╬╬╝╙└\n" +
                "                   \"╙╝╣╬╬╬╬╬╬╬╬╬╬╬╬╬╬╣╝╙╙\n" +
                "     \n\n\n"+
                "                            -RULES- \n\n" +
                "                  Each player starts with 4 cards.\n" +
                "                First one to empty their hand wins.\n" +
                "            Use 'draw' to pick up another card from the deck.\n\n" +
                "--------------------------------------------------------------------------------\n\n";

        while (players.size() != playerNum) {
            clientSocket = serverSocket.accept();
            Player player = new Player(clientSocket);
            players.add(player);
            fixedPool.submit(player);
            player.getOut().write("You have successfully joined!\n\n");
            player.getOut().write(artUno);
            player.getOut().flush();
        }
    }

    public void gameLoop() throws IOException {
        while (!readyToStart()) {
            readyToStart();
        }
        getFirstCard();
        giveHands();

        while (!checkWin()) {
            for (Player player : players) {
                player.getOut().write("\nIt's your turn to play!\n");
                player.getOut().flush();

                for (Player p : players) {
                    checkWin();

                    if (!p.equals(player)) {
                        p.getOut().write("\nIt's " + player.getName() + "'s turn to play!\n");
                        p.getOut().flush();
                    }
                }

                if (!checkWin()) {
                    compareCards(player, player.chooseCard());
                    if (!checkWin()) {
                        showLastCard();
                    }
                } else {
                    fixedPool.shutdown();
                    serverSocket.close();
                    System.exit(0);
                }
                // TODO:do something else
            }
        }
    }

    public boolean readyToStart() {
        int playersReady = 0;
        for (Player p : players) {
            if (p.getName() != null) {
                playersReady++;
            }
        }
        return playersReady == players.size();
    }

    public void getFirstCard() throws IOException {
        int num = (int) (Math.random() * deck.getCards().size());
        lastCardPlayed = deck.getCards().get(num);
        deck.getCards().remove(num);
        for (Player p : players) {
            p.getOut().write("\nStarting card: \n[" + lastCardPlayed.toString() + "]\n");
            p.getOut().flush();
        }
    }

    public void giveHands() {
        for (int i = 0; i < players.size(); i++) {
            for (int j = 0; j < 4; j++) {
                int num = (int) (Math.random() * deck.getCards().size());
                players.get(i).getHand().add(deck.getCards().get(num));
                deck.getCards().remove(num);
            }
        }
    }

    public void showLastCard() throws IOException {
        for (Player p : players) {
            p.getOut().write("\n------------ NEXT PLAY ------------\n");
            p.getOut().write("\nCURRENT CARD: \n[" + lastCardPlayed + "]\n");
            p.getOut().flush();
        }
    }

    public void draw(Player player) throws IOException {
        if (deck.getCards().isEmpty() && discardedPile.isEmpty()) {
            for (Player p : players) {
                p.getOut().write("\n" + player.getName() + " tried to draw, but there are no more cards available in the deck.\n");
                p.getOut().flush();
            }
            return;
        }   else if (deck.getCards().isEmpty()) {
            refillDeck();
        }

        int num = (int) (Math.random() * deck.getCards().size());
        player.getHand().add(deck.getCards().get(num));
        Card card = deck.getCards().remove(num);
        player.getOut().write("You drew [" + card.toString() + "]\n\n");
        player.getOut().flush();
        for (Player p : players) {
            if (!p.equals(player)) {
                p.getOut().write(player.getName() + " drew a card!\n");
            }
        }
    }

    public void refillDeck() {
        int pileSize = discardedPile.size();
        for (int i = 0; i < pileSize; i++) {
            int randomPos = (int) (Math.random() * discardedPile.size());
            deck.setCards(discardedPile.remove(randomPos));
        }
    }


    public void compareCards(Player player, String card) throws IOException {
        if (card.toUpperCase().equals("DRAW")) {
            draw(player);
            return;
        }
        for (int i = 0; i < player.getHand().size(); i++) {
            if (player.getHand().get(i).toString().equals(card)) {
                cardPlayed = player.getHand().get(i);
                break;
            }
        }
        if (cardPlayed.getColor().equals(lastCardPlayed.getColor()) || cardPlayed.getNum() == lastCardPlayed.getNum()) {
            playCard(player);
        } else {
            player.getOut().write("You can't play that card!");
            // sus v
            compareCards(player, player.chooseCard());
        }

    }

    public void playCard(Player player) throws IOException {
        discardedPile.add(cardPlayed);
        lastCardPlayed = cardPlayed;
        player.getOut().write("You played [" + cardPlayed.toString() + "].\n");

        for (Player p : players) {
            if (!p.equals(player)) {
                p.getOut().write(player.getName() + " played [" + cardPlayed.toString() + "].\n");
                p.getOut().flush();
            }
        }
        for (int i = 0; i < player.getHand().size(); i++) {
            if (player.getHand().get(i).equals(cardPlayed)) {
                player.getHand().remove(i);
                break;
            }
        }
    }


    public boolean checkWin() throws IOException {
        boolean win = false;
        String winner = "";
        for (Player p : players) {
            if (p.getHand().size() == 0) {
                winner = p.getName();
                win = true;
                System.out.println("win: " + win);
                win(winner);
                return win;
            }
        }
        return win;
    }

    public void win(String w) throws IOException {
        for (Player p2 : players) {
            p2.getOut().write("\n" + w + " won the game!\n\n" +
                    "888     888 888b    888  .d88888b.  888 \n" +
                    "888     888 8888b   888 d88P\" \"Y88b 888 \n" +
                    "888     888 88888b  888 888     888 888 \n" +
                    "888     888 888Y88b 888 888     888 888 \n" +
                    "888     888 888 Y88b888 888     888 888 \n" +
                    "888     888 888  Y88888 888     888 Y8P \n" +
                    "Y88b. .d88P 888   Y8888 Y88b. .d88P  \"  \n" +
                    " \"Y88888P\"  888    Y888  \"Y88888P\"  888 ");

            p2.getOut().flush();
        }
        System.exit(420);
    }
}