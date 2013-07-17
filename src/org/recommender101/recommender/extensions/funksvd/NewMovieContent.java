package org.recommender101.recommender.extensions.funksvd;

public class NewMovieContent {
	public int id;
	public String title;
	public int Unknown;
	public int Action;
	public int Adventure;
	public int Animation;
	public int Children;
	public int Comedy;
	public int Crime;
	public int Documentary;
	public int Drama;
	public int Fantasy;
	public int FilmNoir;
	public int Horror ;
	public int Musical;
	public int Mystery;
	public int Romance;
	public int SciFi; 
	public int Thriller;
	public int War;
	public int Western; 
	
	public NewMovieContent (NewMovieContent MC ) { 	
			this.id = MC.id;
			System.out.println(MC.id);
			this.Unknown = MC.Unknown;
			this.title = MC.title;
			this.Action = MC.Action;
			this.Adventure = MC.Adventure;
			this.Animation = MC.Animation ;
			this.Children = MC.Children;
			this.Comedy = MC.Comedy;
			this.Crime = MC.Crime;
			this.Documentary = MC.Documentary;
			this.Drama = MC.Drama;
			this.Fantasy = MC.Fantasy;
			this.FilmNoir = MC.FilmNoir;
			this.Horror = MC.Horror ;
			this.Musical = MC.Musical;
			this.Mystery = MC.Mystery;
			this.Romance = MC.Romance;
			this.SciFi = MC.SciFi; 
			this.Thriller = MC.Thriller;
			this.War = MC.War;
			this.Western = MC.Western; 
		
	}
	//Constructor
	public  NewMovieContent(int id,String title,int Unknown,int Action,int Adventure,int Animation,int Children,int Comedy,int Crime,int Documentary,int Drama,int Fantasy,int FilmNoir,int Horror,int Musical,int Mystery,int Romance,int SciFi,int Thriller,int War,int Western) {	
		this.id =  id;
		this.Unknown =  Unknown;
		this.title =  title;
		this.Action =  Action;
		this.Adventure =  Adventure;
		this.Animation =  Animation ;
		this.Children =  Children;
		this.Comedy =  Comedy;
		this.Crime =  Crime;
		this.Documentary =  Documentary;
		this.Drama =  Drama;
		this.Fantasy =  Fantasy;
		this.FilmNoir =  FilmNoir;
		this.Horror =  Horror ;
		this.Musical =  Musical;
		this.Mystery =  Mystery;
		this.Romance =  Romance;
		this.SciFi =  SciFi; 
		this.Thriller =  Thriller;
		this.War =  War;
		this.Western =  Western; 
	}
	
}




