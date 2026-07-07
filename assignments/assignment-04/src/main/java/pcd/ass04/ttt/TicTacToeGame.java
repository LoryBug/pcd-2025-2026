package pcd.ass04.ttt;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TicTacToeGame extends Remote {

    String name() throws RemoteException;

    GameSnapshot snapshot() throws RemoteException;

    MoveResult makeMove(String playerName, int row, int col) throws RemoteException, InvalidMoveException;

    void addListener(TicTacToeListener listener) throws RemoteException;

    void removeListener(TicTacToeListener listener) throws RemoteException;
}
