

//=======================================================================
// IMPORTS
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Random;
import java.awt.event.MouseListener;//so we can listen to the mouse
import java.awt.event.MouseEvent;
//=======================================================================


class MapGenerator {
    
    //=======================================================================
	/**
	* Generating the map. Starts by seeding it randomly with a mountain or live tile here and there,
	* then fills in the gaps. Sets borders as mountains.
	**/
    
    public Tile[][] generateMap(double x, double y, World w, int[] mapIndex){ //generates map of tiles and puts them in the map 2d array
		
		Tile[][] map = new Tile[(int)x][(int)y]; //set map dimensions
		Random r = new Random();//randomize tile life levels with r.nextDouble()*100
		
		int[][] seededMap = generateSeedMap(x, y); //generates seeded map
		
		for(int i = 0; i < map.length; i++){//fill gaps
			for(int j = 0; j < map[0].length; j++){
				Pair tilePos = new Pair(j, i);
				double[] distances = getSeedDistances(seededMap, tilePos);
				if(distances[0]<distances[1]){map[i][j] = new MountainTile(j, i, 0, w, mapIndex);} //if closer to a mountain than a live tile set as mountain
				else{map[i][j] = new Tile(j, i, r.nextDouble()*100+.1, w, mapIndex);} //otherwise set as a live tile with random life.
			}
		}
		
		
		for(int i = 0; i < map.length; i++){
			for(int j = 0; j < map[0].length; j++){
				if(i == 0||i == map.length-1|| j == 0||j == map[0].length-1){
					  map[i][j] = new MountainTile(j, i, 0, w, mapIndex);//put mountains at edges
				}
			}
		}
		
		return map;
	    
	} //generateMap()
	
	public int[][] generateSeedMap(double x, double y){ //creates a 2d array of ints representing mountain seeds, live seeds, and undefined tiles
		Random r = new Random();//generate seeds
		
		int[][] seededMap = new int[(int)x][(int)y];
		double mountainOdds = 0.1; 
		double aliveOdds = 0.2;
		
		for(int i = 0; i < seededMap.length; i++){ //generate seeds
			for(int j = 0; j < seededMap[i].length; j++){
				double rand = r.nextDouble();
				if(rand < mountainOdds){seededMap[i][j] = 0;} //0=mountains   these are also the indexes for distance from that biome later
				else if(rand>mountainOdds && rand<mountainOdds+aliveOdds){seededMap[i][j] = 1;}//1=alive
				else{seededMap[i][j] = 2;}//2=none
			}
		}
		
		return seededMap;
	} //generateSeedMap()
	
	public double[] getSeedDistances(int[][] seededMap, Pair position){ //gets smallest distance of position to mountain and alive seeds
		
		double[] distances = new double[2]; // index 0 = mtn distance index 1 = alive distance
		distances[0]=100000;
		distances[1]=100000; //start em real high or else if there isn't a single seed of 1 type it will always be registered as 0 distance. That would be bad.
		for(int i = 0; i < seededMap.length; i++){ 
			for(int j = 0; j < seededMap[i].length; j++){
				if(!(seededMap[i][j] == 2)){//if the tile is a seed and not nothing
					double distance = Math.sqrt(Math.pow((position.x - j),2) + Math.pow((position.y - i),2));
					if(distance==0.0){//if tile is on a seed make it that seed
						distances[seededMap[i][j]] = 0.0;
						distances[(seededMap[i][j]+1)%2] = 100000.0; //set the other tile type distance to an absurdly high number so it won't become that
						return distances;
					}
					else if(distances[seededMap[i][j]] == 0.0 || distances[seededMap[i][j]] > distance){
						distances[seededMap[i][j]] = distance;
					}
				}
			}
		}
		return distances;
		
	} //getSeedDistances
	//=======================================================================
    
}