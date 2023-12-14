package com.wandermod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

public class AgentPlayerEntity extends BlockEntity {
    private int number = 7;

    static Socket socket = null;
    static PrintWriter out;
    static BufferedReader in;

    boolean hasMap = false;
    int agentCoords[] = {-1,-1};

    public static String sendMessage(String msg){
        out.println(msg);
        String reply = null;
        try {
            reply = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return reply;
    }

    public boolean isConnectedTest(){
        if(socket==null) return false;
        String reply = sendMessage("test");
        return sendMessage("test") != null;
    }

    public boolean connectToSocket(PlayerEntity player){
        try {
            
            if(socket==null||!isConnectedTest()){
                player.sendMessage(Text.literal("Connecting..."), false);
                socket = new Socket("localhost", 9000);
                out = new PrintWriter(socket.getOutputStream(),true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                return socket.isConnected();
            }else{
                return true;
            }
        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
        }
        return false;
    }

    public void restart(){
        sendMessage("restart");
    }

    public static void buildMap(String map,World world,BlockPos pos,int sizeX,int sizeY){
        int n = 0;
        for(int i = 0; i < sizeX; i++){
            for(int j = 0; j < sizeY;j++){
                BlockState block = map.charAt(n) == '0' ? Blocks.GRASS_BLOCK.getDefaultState() : Blocks.AIR.getDefaultState(); 
                world.setBlockState(pos.add(new Vec3i(i,-1,j)),block);
                n++;
            }
        }
    }

    public static void updateAgent(World world,BlockPos pos,int posX,int posY,AgentPlayerEntity entity){
        world.setBlockState(pos.add(new Vec3i(entity.agentCoords[0],0,entity.agentCoords[1])),Blocks.AIR.getDefaultState());
        world.setBlockState(pos.add(new Vec3i(posX,0,posY)),Blocks.RED_WOOL.getDefaultState());
        entity.agentCoords[0] = posX;
        entity.agentCoords[1] = posY;
    }

    public AgentPlayerEntity(BlockPos pos, BlockState state) {
        super(Wandermod.AGENT_PLAYER_ENTITY, pos, state);
    }
    // Serialize the BlockEntity
    @Override
    public void writeNbt(NbtCompound nbt) {
        // Save the current value of the number to the nbt
        nbt.putInt("number", number);
        nbt.putBoolean("hasMap", hasMap);
        nbt.putIntArray("agentCoords", agentCoords);
 
        super.writeNbt(nbt);
    }

    // Deserialize the BlockEntity
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
    
        number = nbt.getInt("number");
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
 
    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
    
    public static void tick(World world, BlockPos pos, BlockState blockState, BlockEntity blockEntity) {
        // Check if the blockEntity from the parameters is actualy this custom BlockEntity
        // You can also just work with a cast instead - we will check for the correct BlockEntity when calling this method anyways
        if (blockEntity instanceof AgentPlayerEntity entity) {
            // Play the sound every 200 ticks with the use of the modulo operator

            if(socket!=null&&socket.isConnected()){
                if(!entity.hasMap){
                    String getMapSizeReply = sendMessage("getMapSize");
                    if(getMapSizeReply!=null){
                        String sizeString[] = getMapSizeReply.split(" ");
                        if(sizeString.length == 2){
                            int sizeX = Integer.parseInt(sizeString[0]);
                            int sizeY = Integer.parseInt(sizeString[1]);
                            String getMapReply = sendMessage("getMap");
                            if(getMapReply!=null){
                                buildMap(getMapReply, world, pos, sizeX, sizeY);
                                entity.hasMap = true;
                            }
                        
                        }
                    }
           
                }else{
                    // for(PlayerEntity player:  world.getPlayers()){
                    //     player.sendMessage(Text.literal("Update agent"), false);
                    // }
                    String getAgentReply = sendMessage("getAgent");
                    if(getAgentReply!=null){
                        String posString[] = getAgentReply.split(" ");
                        if(posString.length == 2){
                            int posX = Integer.parseInt(posString[0]);
                            int posY = Integer.parseInt(posString[1]);

                            updateAgent(world, pos, posX, posY, entity);
                        }
                    }

                }
        
            }
            // socketServer.tickListen();
            // Increment the tick attribute of the instance
            // entity.number++;
        }
    }
}