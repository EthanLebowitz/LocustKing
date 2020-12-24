//=======================================================================
/**
 * This is the World Class of the Plague! game. 
 * This class is responsible for keeping track of the boids and the map.
 * From here we call the draw() functions of all the boids and tiles, as well
 * as call update() on the boids. World is also responsible for map generation.
 */
//=======================================================================



//=======================================================================
// IMPORTS
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashMap;
//import java.util.Hashtable;
import java.util.Random;
import java.awt.event.MouseListener;//so we can listen to the mouse
import java.awt.event.MouseEvent;
//=======================================================================


//=======================================================================
class World{
//=======================================================================
	
	
	//=======================================================================
    /**
     *@param worldDimensions dimensions of the world in tiles
     *@param mainInstance instance of Main
     *@param initNumBoids starting number of locusts
     *@param numBoids int keeping track of the current number of locusts
     *@param boids ArrayList of all the locusts
     *@param kingBoid contains the instance of KingBoid
     *@param map 2D array of the tiles
     **/
    Pair mapDimensions;
	Main mainInstance;
    MapGenerator mapGenerator;
    
	int initNumBoids = 1; //starting number of locusts to be generated
	int numBoids = 0; //keeps track of how many locusts there are
	public ArrayList<Locust> boids = new ArrayList<Locust>(); //ArrayList containing alll the locusts
    
	KingBoid kingBoid = new KingBoid(2000,2000,this); 
    public HashMap<String, Chunk> chunks;
	//public Tile[][] map; // 2D array will contain all the tiles. Their positions in the array represent their positions in the game
	//=======================================================================

	
	//=======================================================================
    /**
     * Constructor for world. Sets fields, calls generateMap() and 
	 * creates the initial number of locusts near the king
     **/
    public World(Pair worldDimensions, Main mainInstance){
		this.mapDimensions = new Pair(Chunk.chunkWidth, Chunk.chunkHeight);
        //maps = new Hashtable<String,Tile[][]>();
        chunks = new HashMap<>();
        int[] origin = {0,0};
        Chunk firstChunk = new Chunk(origin, this);
        chunks.put(convertMapIndexToKey(origin), firstChunk);
		this.mainInstance = mainInstance;
		Random r = new Random();
		for(int i = 0; i < initNumBoids; i++){ //create boids near the king
		    boids.add(new Locust((r.nextDouble()-.5)*400 + kingBoid.position.x, (r.nextDouble()-.5)*400 + kingBoid.position.y, this));
		}
	
	} //World() constructor
	//=======================================================================
	
	
	//=======================================================================
	/**
	*Returns the percent of tiles left alive. Calls win() in Main if none are left.
	**/
	/* public int getPercentAlive(){ 
		int alive = 0;
		int dead = 0;
		for(int i = 0; i < map.length; i++){ //iterates through all tiles
			for(int j = 0; j < map[0].length; j++){
				if(map[i][j].alive){
					alive++;
				}
				else if(map[i][j].isMountain == false && map[i][j].alive == false){ //mountains don't count towards the dead tile count
					dead++;
				}
			}
		}
		double percent = (((double)alive/(double)(alive+dead))*100);
		int percentInt = (int)percent;
		if(alive == 0){mainInstance.win();} //if there are none alive win
		return percentInt;
	} //getPercentAlive() */
	//=======================================================================

	public String convertMapIndexToKey(int[] index){
        String key = Integer.toString(index[0]) + ":" + Integer.toString(index[1]);
        return key;
    }
    
     public String checkForEdge(Chunk chunk){ //
        
        Tile currentTile = kingBoid.getTile(chunk);
        Pair tilePosition = currentTile.position;
        String edgesString = "";
        
        int displayWidthInTiles = mainInstance.WIDTH / Tile.width;
        int displayHeightInTiles = mainInstance.HEIGHT / Tile.height;
        
        if(tilePosition.x < displayWidthInTiles){edgesString = edgesString + "L";}
        if(tilePosition.x > (this.mapDimensions.x - displayWidthInTiles)){edgesString = edgesString + "R";}
        if(tilePosition.y < displayHeightInTiles){edgesString = edgesString + "T";}
        if(tilePosition.y > (this.mapDimensions.y - displayHeightInTiles)){edgesString = edgesString + "B";}
        
        return edgesString;
        
    } 
    
    public Chunk getCurrentChunk(){
        
        Pair positionByTiles = new Pair(kingBoid.position.x/Tile.width, kingBoid.position.y/Tile.height);
        int[] mapIndex = {(int)Math.floor(positionByTiles.x/this.mapDimensions.x), (int)Math.floor(positionByTiles.y/this.mapDimensions.y)};
        Chunk currentChunk = this.chunks.get(convertMapIndexToKey(mapIndex));
        currentChunk.initializeAdjacentChunks();
        return currentChunk;
        
    }
    
	//=======================================================================
	/**
	*Calling the draw functions of all the Boids and Tiles.
	**/
    public void drawBoids(Graphics g, Main mainInstance){ //calls every locust's draw() method as well as the king's
	
		for (int i = 0; i < numBoids; i++){
		    boids.get(i).draw(g, this);
		}
		kingBoid.draw(g, mainInstance);
		
    }
	
	public void drawTiles(Graphics g, Main mainInstance){  //iterates through all the tiles and calls their draw functions.
    
        /* String nearEdges = checkForEdge();
        if(! nearEdges.equals("")){ //if we are near an edge
            generateAdjacentMaps(nearEdges);
        } */
        
        //but first find which map(s) we're in
        Chunk currentChunk = getCurrentChunk();
        System.out.println(convertMapIndexToKey(currentChunk.position));
        ArrayList<Chunk> chunksInView = new ArrayList<>();
        chunksInView.add(currentChunk);
        System.out.println(kingBoid.getTile(currentChunk).position.y);
        String nearEdges = checkForEdge(currentChunk);
        System.out.println(nearEdges);
        if(nearEdges.contains("T")){
            chunksInView.add(currentChunk.chunkUp);
        }if(nearEdges.contains("L")){
            chunksInView.add(currentChunk.chunkLeft);
        }if(nearEdges.contains("R")){
            chunksInView.add(currentChunk.chunkRight);
        }if(nearEdges.contains("B")){
            chunksInView.add(currentChunk.chunkDown);
        }if(nearEdges.equals("RT")){
            chunksInView.add(currentChunk.chunkUpperRight);
        }if(nearEdges.equals("RB")){
            chunksInView.add(currentChunk.chunkLowerRight);
        }if(nearEdges.equals("LT")){
            chunksInView.add(currentChunk.chunkUpperLeft);
        }if(nearEdges.equals("LB")){
            chunksInView.add(currentChunk.chunkLowerLeft);
        }
        
        for(var c = 0; c<chunksInView.size(); c++){
            Tile[][] map = chunksInView.get(c).map;
            for(int i = 0; i < map.length; i++){
                for(int j = 0; j < map[0].length; j++){
                    map[i][j].draw(g, kingBoid.position, mainInstance);
                }
            }
        }
	}
	//=======================================================================
	
    /* public Pair[] getMaps(kingBoid.position){
        
        int offscreenTiles = 3; //number of offscreen tiles to display to avoid rendering jitters around edges
		double tilesWide = (this.mainInstance.WIDTH / Tile.width);
	    double tilesHigh = (this.mainInstance.HEIGHT / Tile.height);
		Pair displayXBounds = new Pair(displayCenter.x-((Main.WIDTH/2)+(offscreenTiles*Tile.width)), displayCenter.x+((Main.WIDTH/2)+(offscreenTiles*Tile.width))); //left and right bounds respectively
		Pair displayYBounds = new Pair(displayCenter.y-((Main.HEIGHT/2)+(offscreenTiles*Tile.height)), displayCenter.y+((Main.HEIGHT/2)+(offscreenTiles*Tile.height))); //top and bottom bounds respectively
		if(tileCoordX > displayXBounds.x  && tileCoordX < displayXBounds.y ){
			if(tileCoordY > displayYBounds.x && tileCoordY < displayYBounds.y ){
				return true;
			}
		}
		return false; //tiles over 3 tile spaces out of bounds are not drawn to preserve framerate
        
    } */

	//=======================================================================
	/**
	*Calling the update functions of all the Boids.
	**/
    public void updateBoids(double time){ //calls every boid's update() method. If there aren't any locusts left calls the lose() method of Main. 
        Chunk currentChunk = getCurrentChunk();
		kingBoid.update(time, currentChunk, mainInstance.mousePosition);//here's where we call the mouse listener from the main method
		for (int i = 0; i < numBoids; i ++){
		    boids.get(i).update(time, currentChunk, boids, kingBoid);
		}
		if(this.numBoids == 0){
			mainInstance.lose();
		}
	}
	//=======================================================================
	
	
	//=======================================================================
	/**
	* Convert coordinates in the world to coordinates on the screen. Static so that it can be used by any object.
	**/
	public static Pair toDisplayCoords(Pair coords, Pair displayCenter){ 
		
		double tileCoordX = (coords.x);
		double tileCoordY = (coords.y); //these are coords of the top left corner
		Pair topLeftDisplayCorner = new Pair(displayCenter.x-(Main.WIDTH/2), displayCenter.y-(Main.HEIGHT/2));
		double relativeX = tileCoordX - topLeftDisplayCorner.x;
		double relativeY = tileCoordY - topLeftDisplayCorner.y;
		Pair displayCoords = new Pair(relativeX, relativeY);
		return displayCoords;
		
	} //toDisplayCoords()
	//=======================================================================

} //class World
