package pcd.ass04.ttt;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface TicTacToeHub extends Remote {

    TicTacToeGame createGame(String gameName, String playerName, TicTacToeListener listener)
            throws RemoteException;

    TicTacToeGame joinGame(String gameName, String playerName, TicTacToeListener listener)
            throws RemoteException;

    List<String> listGames() throws RemoteException;
}
