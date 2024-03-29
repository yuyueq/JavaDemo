package alogrithm;

import chess.Board;
import chess.Piece;
import chess.Rules;
import control.GameController;

import java.util.ArrayList;
import java.util.Map;

public class SearchModel {
	private static int DEPTH = 2;
	private Board board;
	private GameController controller = new GameController();

	public AlphaBetaNode search(Board board) {
		this.board = board;
		if (board.pieces.size() < 28)
			DEPTH = 3;
		if (board.pieces.size() < 16)
			DEPTH = 4;
		if (board.pieces.size() < 6)
			DEPTH = 5;
		if (board.pieces.size() < 4)
			DEPTH = 6;
		long startTime = System.currentTimeMillis();
		AlphaBetaNode best = null;
		ArrayList<AlphaBetaNode> moves = generateMovesForAll(true);
		for (AlphaBetaNode n : moves) {
			/* 移动 */
			Piece eaten = board.updatePiece(n.piece, n.to);
			n.value = alphaBeta(DEPTH, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
			/* 在搜索过程中选择一个最好的步骤来节省时间 */
			if (best == null || n.value >= best.value)
				best = n;
			/* 后移 */
			board.updatePiece(n.piece, n.from);
			if (eaten != null) {
				board.pieces.put(eaten.key, eaten);
				board.backPiece(eaten.key);
			}
		}
		long finishTime = System.currentTimeMillis();
		System.out.println(finishTime - startTime);
		return best;
	}

	private int alphaBeta(int depth, int alpha, int beta, boolean isMax) {
		/* 如果达到叶节点或任何一方赢了还评价。. */
		if (depth == 0 || controller.hasWin(board) != 'x')
			return new EvalModel().eval(board, 'b');
		ArrayList<AlphaBetaNode> moves = generateMovesForAll(isMax);

		synchronized (this) {
			for (final AlphaBetaNode n : moves) {
				Piece eaten = board.updatePiece(n.piece, n.to);
				/* 是最大化的游戏者? */
				final int finalBeta = beta;
				final int finalAlpha = alpha;
				final int finalDepth = depth;
				final int[] temp = new int[1];
				/* 深度2只采用多线程策略。为了避免结合. */
				if (depth == 2) {
					if (isMax) {
						new Thread(new Runnable() {
							@Override
							public void run() {
								temp[0] = Math.max(finalAlpha, alphaBeta(finalDepth - 1, finalAlpha, finalBeta, false));
							}
						}).run();
						alpha = temp[0];
					} else {
						new Thread(new Runnable() {
							@Override
							public void run() {
								temp[0] = Math.min(finalBeta, alphaBeta(finalDepth - 1, finalAlpha, finalBeta, true));
							}
						}).run();
						beta = temp[0];
					}
				} else {
					if (isMax)
						alpha = Math.max(alpha, alphaBeta(depth - 1, alpha, beta, false));
					else
						beta = Math.min(beta, alphaBeta(depth - 1, alpha, beta, true));
				}
				board.updatePiece(n.piece, n.from);
				if (eaten != null) {
					board.pieces.put(eaten.key, eaten);
					board.backPiece(eaten.key);
				}
				/* 截止 */
				if (beta <= alpha)
					break;
			}
		}
		return isMax ? alpha : beta;
	}

	private ArrayList<AlphaBetaNode> generateMovesForAll(boolean isMax) {
		ArrayList<AlphaBetaNode> moves = new ArrayList<AlphaBetaNode>();
		for (Map.Entry<String, Piece> stringPieceEntry : board.pieces.entrySet()) {
			Piece piece = stringPieceEntry.getValue();
			if (isMax && piece.color == 'r')
				continue;
			if (!isMax && piece.color == 'b')
				continue;
			for (int[] nxt : Rules.getNextMove(piece.key, piece.position, board))
				moves.add(new AlphaBetaNode(piece.key, piece.position, nxt));
		}
		return moves;
	}

}