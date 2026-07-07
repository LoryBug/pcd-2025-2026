package pcd.ass04.ttt;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TicTacToeListener extends Remote {

    void onPlayerJoined(String gameName, String playerName, Mark mark) throws RemoteException;

    void onMove(String gameName, String playerName, int row, int col, Mark mark) throws RemoteException;

    void onGameOver(String gameName, GameSnapshot snapshot) throws RemoteException;
}
