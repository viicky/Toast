package gameElements;

import java.util.List;
import java.util.Observable;

public class Game extends Observable {
	
	private Board board;
	private int cptMCTS;
	private double chancesVictoire;
	private ArbreMonteCarlo prochainCoup;
	
	private boolean calculOrdi; //indique si l'ordi est en train de calculer

	private int gagnant;

	public Game() {
		board = new Board();
		gagnant = Board.WHITE;
		calculOrdi = false;
	}
	
	/*
	 * Prévient les vues que le modèle a été modifié
	 */
	private void maj(){
		setChanged();
		notifyObservers();
	}
	
	public int getWidth(){
		return board.getWidth();
	}
	
	public int getHeight(){
		return board.getHeight();
	}
	
	/*
	 * Remet le plateau à 0
	 */
	public void reset(int width, int height){
		board = new Board(width, height);
		gagnant = Board.WHITE;
		maj();
	}
	
	/*
	 * Renvoie la ligne d'une case accessible de colonne x
	 * Ou -1 si la colonne est pleine
	 */
	public int selectionnerCaseAccessible(int x){
		return board.selectionnerCaseAccessible(x);
	}
	
	/*
	 * Pose un pion dans la colonne x
	 * Ne fait rien si la colonne est pleine
	 */
	public void poserPion(int x){
		//ne rien faire si le jeu est fini
		if(gagnant != Board.WHITE){
			return;
		}
		if(board.poserPion(x)){
			setJoueurActuel(Board.YELLOW);
			gagnant = board.isFinal();
		
			//tour de l'ordi
			if(gagnant != Board.WHITE) return;
			if(forcerOrdi(Board.YELLOW) || forcerOrdi(Board.RED)){
				maj();
				return;
			}
			
			
			//lancer par avance le calcul de MCTS
			Thread t = new Thread(new MCTSLauncher(this));
			t.start();
		}
		maj();
	}
	
	private boolean forcerOrdi(int couleur){
		List<Board> succ = board.successeurs(couleur);
		for(Board b : succ){
			if (b.isFinal() == couleur){
				board.poserPion(b.getDernierXPose());
				setJoueurActuel(Board.RED);
				if(couleur == Board.YELLOW){
					chancesVictoire = 1;
					gagnant = Board.YELLOW;
				}
				else {
					chancesVictoire = -1; // -1 car on ne connait pas ses chances de Victoire
				}
				return true;
			}
		}
		return false;
	}
	
	/*
	 * L'ordi pose un pion dans une colonne au hasard.
	 * Peut échouer si la colonne choisie est remplie
	 */
	synchronized public void ordiQuiJoue(){
		//ne rien faire si le jeu est fini
		if(gagnant != Board.WHITE){
			return;
		}
		
		//attendre que le calcul se termine
		while(calculOrdi){
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		ArbreMonteCarlo plusGrand = prochainCoup.meilleureMoyenne();
		board = plusGrand.getBoard();
		gagnant = board.isFinal();
		chancesVictoire = plusGrand.mu();
		
		setJoueurActuel(Board.RED);
		maj();
	}

	public Board getBoard() {
		return board;
	}

	public void setBoard(Board board) {
		this.board = board;
	}
	
	public void setGagnant(int gagnant) {
		this.gagnant = gagnant;
	}

	public int getGagnant() {
		return gagnant;
	}

	public int getJoueurActuel() {
		return board.getJoueurActuel();
	}

	public void setJoueurActuel(int joueurActuel) {
		board.setJoueurActuel(joueurActuel);
	}
	
	public int getCptMCTS() {
		return cptMCTS;
	}

	public String toString(){
		return board.toString();
	}
		
	public int getCell(int x, int y){
		return board.getCell(x, y);
	}

	public ArbreMonteCarlo getProchainCoup() {
		prochainCoup = new ArbreMonteCarlo(board);
		return prochainCoup;
	}
	
	public void setProchainCoup(ArbreMonteCarlo arbre){
		this.prochainCoup = arbre;
	}
	
	public void setCptMCTS(int cpt){
		this.cptMCTS = cpt;
	}
	
	
	public double getChancesVictoire() {
		return chancesVictoire;
	}

	public void setChancesVictoire(double chancesVictoire) {
		this.chancesVictoire = chancesVictoire;
	}

	synchronized public void attendre(){
		calculOrdi = true;
	}
	
	synchronized public void finCalculOrdi(){
		calculOrdi = false;
		notify();
	}
}
