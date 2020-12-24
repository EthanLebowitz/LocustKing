class Chunk {
    
    public static final int chunkWidth = 50;
    public static final int chunkHeight = 50;
    Tile[][] map;
    int[] position; //chunk position on map of chunks
    boolean adjacentsInitialized = false;
    
    //links
    Chunk chunkUpperLeft;
    Chunk chunkUp;
    Chunk chunkUpperRight;
    Chunk chunkRight;
    Chunk chunkLowerRight;
    Chunk chunkDown;
    Chunk chunkLowerLeft;
    Chunk chunkLeft;
    // end links
    
    World world;
    
    public Chunk(int[] pos, World w){
        
        MapGenerator mapGenerator = new MapGenerator();
        this.world = w;
        this.position = pos;
		this.map = mapGenerator.generateMap(chunkWidth, chunkHeight, w, pos);
        
        for(var i=-1; i<2; i++){ //see if the 8 adjacent chunks are loaded and if so link
            for(var j=-1; j<2; j++){
                int[] adjChunkPosition = {pos[0]+i, pos[1]+j};
                String key = this.world.convertMapIndexToKey(adjChunkPosition);
                if(world.chunks.containsKey(key)){ //if the chunk exists
                    Chunk adjChunk = world.chunks.get(key);
                    this.addLink(adjChunk);
                    adjChunk.addLink(this);
                }
            }
        }
    }
    
    public void initializeAdjacentChunks(){
        if(!adjacentsInitialized){
            if(chunkUpperLeft == null){
                int[] adjPosition = {this.position[0]-1, this.position[1]-1};
                chunkUpperLeft = new Chunk(adjPosition, world);
                String key = world.convertMapIndexToKey(adjPosition);
                world.chunks.put(key, chunkUpperLeft);
            }if(chunkUp == null){
                int[] adjPosition = {this.position[0], this.position[1]-1};
                chunkUp = new Chunk(adjPosition, world);
                String key = world.convertMapIndexToKey(adjPosition);
                world.chunks.put(key, chunkUp);
            }if(chunkUpperRight == null){
                int[] adjPosition = {this.position[0]+1, this.position[1]-1};
                chunkUpperRight = new Chunk(adjPosition, world);
                String key = world.convertMapIndexToKey(adjPosition);
                world.chunks.put(key, chunkUpperRight);
            }if(chunkRight == null){
                int[] adjPosition = {this.position[0]+1, this.position[1]};
                chunkRight = new Chunk(adjPosition, world);
                String key = world.convertMapIndexToKey(adjPosition);
                world.chunks.put(key, chunkRight);
            }if(chunkLowerRight == null){
                int[] adjPosition = {this.position[0]+1, this.position[1]+1};
                chunkLowerRight = new Chunk(adjPosition, world);
                String key = world.convertMapIndexToKey(adjPosition);
                world.chunks.put(key, chunkLowerRight);
            }if(chunkDown == null){
                int[] adjPosition = {this.position[0], this.position[1]+1};
                chunkDown = new Chunk(adjPosition, world);
                String key = world.convertMapIndexToKey(adjPosition);
                world.chunks.put(key, chunkDown);
            }if(chunkLowerLeft == null){
                int[] adjPosition = {this.position[0]-1, this.position[1]+1};
                chunkLowerLeft = new Chunk(adjPosition, world);
                String key = world.convertMapIndexToKey(adjPosition);
                world.chunks.put(key, chunkLowerLeft);
            }if(chunkLeft == null){
                int[] adjPosition = {this.position[0]-1, this.position[1]};
                chunkLeft = new Chunk(adjPosition, world);
                String key = world.convertMapIndexToKey(adjPosition);
                world.chunks.put(key, chunkLeft);
            }
        }
    }
    
    public void addLink(Chunk chunk){
        
        int[] adjPosition = chunk.position;
        int xDiff = this.position[0] - adjPosition[0];
        int yDiff = this.position[1] - adjPosition[1];
        
        if(xDiff < 0){ //if xDiff is negative then the other chunk is to the right
            if(yDiff < 0){ //below
                this.chunkLowerRight = chunk;
            }else if(yDiff == 0){ //direct
                this.chunkRight = chunk;
            }else if(yDiff > 0){ //above
                this.chunkUpperRight = chunk;
            }
        }else if(xDiff > 0){ //left
            if(yDiff < 0){ //below
                this.chunkLowerLeft = chunk;
            }else if(yDiff == 0){ //direct
                this.chunkLeft = chunk;
            }else if(yDiff > 0){ //above
                this.chunkUpperLeft = chunk;
            }
        }else{ //up or down
            if(yDiff < 0){ //below
                this.chunkDown = chunk;
            }else if(yDiff > 0){ //above
                this.chunkUp = chunk;
            }
        }
        
    }
    
}