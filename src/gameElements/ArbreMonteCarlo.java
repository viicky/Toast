package gameElements;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import util.FloatUtility;

/*
 * Un arbre de Monte-Carlo, qui permet en un temps limité de trouver le meilleur coup à jouer
 */
public class ArbreMonteCarlo {
	private int Ni;
	private static final double C = Math.sqrt((double) 2);
	private double Si;
	
	private ArbreMonteCarlo parent;
	private List<ArbreMonteCarlo> filsNonDeveloppes;
	private List<ArbreMonteCarlo> filsDeveloppes;
	
	private boolean developpe;
		
	private Board board;
	
	/*
	 * Moyenne des récompenses
	 */
	public double mu(){
		return Si / (double) Ni; 
	}
	
	/*
	 * Nombre de passages par le noeud
	 */
	private int getNi(){
		return Ni;
	}
	
	/*
	 * Nombre de passages par le noeud du parent
	 */
	private int NParent(){
		if(parent == null) return 0;
		return parent.getNi();
	}

	
	public ArbreMonteCarlo(Board bo){
		board = bo;
		Ni = 0;
		Si = 0;
		filsDeveloppes = new ArrayList<>();
		filsNonDeveloppes = new ArrayList<>();
		developpe = false;
	}

	
	/*
	 * Mise à jour de la BValeur en fonction de la récompense
	 */
	private void majBValeur(int r){
		Si += (double) r;
		Ni++;
	}
	
	/*
	 * BValeur
	 */
	private double getBValeur(){
		return mu() + C * Math.sqrt(Math.log(NParent()) / Ni);
	}
	
	/*
	 * Récupérer les fils sans BValeur
	 */
	public List<ArbreMonteCarlo> getFilsNonDeveloppes(){
		return filsNonDeveloppes;
	}
	
	/*
	 * Récupérer les fils avec une BValeur
	 */
	private List<ArbreMonteCarlo> getFilsDeveloppes(){
		return filsDeveloppes;
	}
	
	private List<ArbreMonteCarlo> getFils(){
		final int taille = filsDeveloppes.size()+filsNonDeveloppes.size();
		List<ArbreMonteCarlo> rep = new ArrayList<ArbreMonteCarlo>(taille);
		
		for(ArbreMonteCarlo a : filsDeveloppes){
			rep.add(a);
		}
		for(ArbreMonteCarlo a : filsNonDeveloppes){
			rep.add(a);
		}
		
		return rep;
	}

	
	/*
	 * Récupérer le noeud de plus grande B-Valeur
	 */
	public ArbreMonteCarlo selecPlusGrandeBValeur(){
		double BMax = -1;
		ArbreMonteCarlo meilleur = null;
		for(ArbreMonteCarlo a : getFils()){
			if(BMax < a.getBValeur()){
				BMax = a.getBValeur();
				meilleur = a;
			}
		}
		return meilleur;
	}
	
	
	private boolean possedeNonDeveloppe(){
		return filsNonDeveloppes.size() > 0;
	}
	
	private boolean estDeveloppe(){
		return developpe;
	}
	
	/*
	 * Un noeud est terminal si son état est final
	 */
	private boolean estTerminal(){
		return board.isFinal() != Board.WHITE;
	}
	
	/*
	 * Déroule une partie avec des coups aléatoires.
	 * Renvoie la récompense de cette partie
	 * (voir Board::marcheAleatoire)
	 */
	private int marcheAleatoire(){
		return board.marcheAleatoire();
	}
	
	public void MCTS(){
		if (estTerminal()){
			marcheAleatoireEtMiseAJour();
		}
		if (possedeNonDeveloppe()){
			/* Developpement de C3 (cf cours) */
			List<ArbreMonteCarlo> lesFils = getFilsNonDeveloppes();
			int index = (int)Math.random()*lesFils.size();
			ArbreMonteCarlo fils = lesFils.get(index);
			fils.developper();
			
			/* Developpement de C31 */
			List<ArbreMonteCarlo> lesPetitsFils = fils.getFilsNonDeveloppes();
			if (lesPetitsFils.isEmpty()){
				fils.marcheAleatoireEtMiseAJour();
				return;  // pas de petit fils donc marche aleatoire sur fils
			}
			index = (int)Math.random()*lesPetitsFils.size();
			ArbreMonteCarlo petitFils = lesPetitsFils.get(index);
			
			/* Marche Aleatoire + Mettre a jour par backtracking */
			petitFils.marcheAleatoireEtMiseAJour();
		}
		//appels récursifs
		ArbreMonteCarlo suivant = selecPlusGrandeBValeur();
		if(suivant != null){
			suivant.MCTS();
		}
		
	}
	
	private void marcheAleatoireEtMiseAJour(){
		ArbreMonteCarlo noeudActuel = this;
		int marcheAlea = noeudActuel.marcheAleatoire();
		
		while (noeudActuel != null){
			noeudActuel.majBValeur(marcheAlea);
			noeudActuel = noeudActuel.parent;
		}
	}
	
	private void developper(){
		filsNonDeveloppes = new ArrayList<>();
		for(Board b : board.successeurs()){
			ArbreMonteCarlo arbre = new ArbreMonteCarlo(b);
			arbre.setParent(this);
			filsNonDeveloppes.add(new ArbreMonteCarlo(b));
		}
		developpe = true;
		if(parent != null){ //mettre à jour la liste du parent
			parent.nouveauDeveloppe(this);
		}
	}
	
	private void nouveauDeveloppe(ArbreMonteCarlo a){
		filsDeveloppes.add(a);
		filsNonDeveloppes.remove(a);
	}
	
	
	public Board getBoard() {
		return board;
	}

	private void setParent(ArbreMonteCarlo parent) {
		this.parent = parent;
	}

	public static void main(String[] args){
		ArbreMonteCarlo a = new ArbreMonteCarlo(new Board(2, 2));
		
		//test du nombre de fils (successeurs)
		a.developper();
		List<ArbreMonteCarlo> succs = a.getFils();
		assert(succs.size() == 2):"Mauvais nombre de successeurs";
		
		//maj de BValeur
		a.majBValeur(1); //victoire
		assert(a.mu() == 1f):"Mauvaise moyenne des récompenses";
		a.majBValeur(1); //victoire
		assert(a.mu() == 1f):"Mauvaise moyenne des récompenses";
		a.majBValeur(0); //défaite
		assert(FloatUtility.near(a.mu(), 2d/3d)):"Mauvaise moyenne des récompenses";
		assert(a.getNi() == 3):"Mauvais comptage du nombre de mises à jour";
		
		System.out.println("OK");
	}

}
