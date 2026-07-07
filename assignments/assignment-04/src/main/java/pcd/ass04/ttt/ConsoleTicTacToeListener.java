package pcd.ass04.ttt;

import java.rmi.RemoteException;

public final class ConsoleTicTacToeListener implements TicTacToeListener {

    @Override
    public void onPlayerJoined(String gameName, String playerName, Mark mark) {
        System.out.printf("[%s] %s joined as %s%n", gameName, playerName, mark);
    }

    @Override
    public void onMove(String gameName, String playerName, int row, int col, Mark mark) {
        System.out.printf("[%s] %s played %s at (%d,%d)%n", gameName, playerName, mark, row, col);
    }

    @Override
    public void onGameOver(String gameName, GameSnapshot snapshot) throws RemoteException {
        System.out.printf("[%s] game over: %s%n", gameName, snapshot);
    }
}
