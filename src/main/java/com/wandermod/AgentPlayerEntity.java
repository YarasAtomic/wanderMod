package com.wandermod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item.Settings;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class AgentPlayerEntity extends BlockEntity {


    static Socket socket = null;
    static PrintWriter out = null;
    static BufferedReader in = null;

    //NBT
    static boolean hasMap = false;
    int agentCoords[] = {-1,-1};
    UUID agentId = null;
    UUID endId = null;
    List<UUID> objetivesId = new ArrayList<UUID>();
    int objetiveCount = 0;
    int number = 0;

    // Other
    int lastPower = 0;

    public void activate(World world, BlockPos pos,PlayerEntity player){
        hasMap = false;
        if(connectToSocket( player)){
            if(player!=null){
                player.sendMessage(Text.literal("Agent run"), false);
            }
            
            world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1f, 1f);
            restart();
            destroyObjetives(world,pos);
            setObjetives(world, pos);
            summonAgent(world,  pos);
            setEnd(world, pos);
            markDirty();
        }

    }

    public static String sendMessage(String msg){
        String reply = null;
        if(socket!=null){
            out.println(msg);
            try {
                reply = in.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return reply;
    }

    public boolean isConnectedTest(){
        if(socket==null) return false;
        return sendMessage("test") != null;
    }

    public boolean connectToSocket(PlayerEntity player){
        try {
            
            if(socket==null||!isConnectedTest()){
                hasMap=false;
                if(player!=null){
                    player.sendMessage(Text.literal("Connecting..."), false);
                }
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

    public void summonAgent(World world, BlockPos pos){
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof AgentPlayerEntity entity) {
            if(entity.agentId==null){
                ZombieEntity agentEntity = new ZombieEntity(EntityType.ZOMBIE, world);

                agentEntity.setPosition(new Vec3d(entity.agentCoords[0]+pos.getX()+0.5,-1+pos.getY(),entity.agentCoords[1]+pos.getZ()+0.5));
                agentEntity.setInvulnerable(true);
                agentEntity.setAiDisabled(true);
                ItemStack is = new ItemStack(Registries.ITEM.get(new Identifier("minecraft","iron_helmet")),1);
                
                NbtCompound nbt = new NbtCompound();
                nbt.putBoolean("Unbreakable", true);
                is.setNbt(nbt);
                agentEntity.equipStack(EquipmentSlot.HEAD, is);
                entity.agentId = agentEntity.getUuid();
                world.spawnEntity(agentEntity);
            }
        }
    }

    public void summonEnd(World world, BlockPos blockPos,Vec3d pos){
        VillagerEntity endEntity = new VillagerEntity(EntityType.VILLAGER, world);

        endEntity.setPosition(new Vec3d(pos.getX()+blockPos.getX()+0.5,pos.getY()+blockPos.getY(),pos.getZ()+blockPos.getZ()+0.5));
        endEntity.setInvulnerable(true);
        endEntity.setAiDisabled(true);
        // ItemStack is = new ItemStack(Registries.ITEM.get(new Identifier("minecraft","sponge")),1);
        // agentEntity.equipStack(EquipmentSlot.HEAD, is);
        endId = endEntity.getUuid();
        world.spawnEntity(endEntity);
    }

    public void setObjetives(World world, BlockPos pos){
        String getObjetivesReply = sendMessage("getObjetives");
        if(getObjetivesReply!=null){
            updateObjetives(getObjetivesReply,this,world,pos);
        }
    }

    public void setEnd(World world, BlockPos pos){
        String getEndReply = sendMessage("getEnd");
        Vec3d endPos = Utils.stringToVectord(getEndReply);
        if(endPos!=null){
            if(endId==null){
                summonEnd(world,pos,endPos);
            }else{
                VillagerEntity endEntity = Utils.getEntityByUuid(world, endId, pos,64,VillagerEntity.class);
                if(endEntity!=null){
                    endEntity.setPos((double)(pos.getX()+endPos.getX())+0.5, pos.getY(), (double)(pos.getZ()+endPos.getZ())+0.5);
                }else{
                    summonEnd(world,pos,endPos);
                }
            }
        }
    }

    public static void buildMap(String map,World world,BlockPos pos,int sizeX,int sizeY){
        if(map.length() == sizeX*sizeY){
            int n = 0;
            for(int i = 0; i < sizeX; i++){
                for(int j = 0; j < sizeY;j++){
                    BlockState block = map.charAt(n) == '0' ? Blocks.GRASS_BLOCK.getDefaultState() : Blocks.AIR.getDefaultState(); 
                    world.setBlockState(pos.add(new Vec3i(i,-1,j)),block);
                    n++;
                }
            }
        }

    }

    public static void updateAgent(World world,BlockPos pos,int posX,int posY,AgentPlayerEntity entity){
        // world.setBlockState(pos.add(new Vec3i(entity.agentCoords[0],0,entity.agentCoords[1])),Blocks.AIR.getDefaultState());
        // world.setBlockState(pos.add(new Vec3i(posX,0,posY)),Blocks.RED_WOOL.getDefaultState());

        if(entity.agentId!=null){
            ZombieEntity agentEntity = Utils.getEntityByUuid(world, entity.agentId, pos, 64,ZombieEntity.class);

            if(agentEntity!=null){
                float angle = -1;
                if(posX < entity.agentCoords[0]){
                    angle = 90;
                }else if(posX > entity.agentCoords[0]){
                    angle = 270;
                }else if(posY < entity.agentCoords[1]){
                    angle = 180;
                }else if(posY > entity.agentCoords[1]){
                    angle = 0;
                }
                entity.agentCoords[0] = posX;
                entity.agentCoords[1] = posY;
                // agentEntity.updateTrackedPositionAndAngles(pos.getX()+posX+0.5, pos.getY(),pos.getZ()+ posY+0.5,0.0f,(float)entity.number,1);
                agentEntity.setPos(pos.getX()+posX+0.5, pos.getY(),pos.getZ()+ posY+0.5);
                if(angle!=-1){
                    agentEntity.setBodyYaw(angle);
                    agentEntity.setHeadYaw(angle);
                }

                // agentEntity.setBodyYaw(angle);
                // agentEntity.setPitch(entity.number);
            }else{
                entity.agentId=null;
            }
        }
    }

    public void destroyObjetives(World world,BlockPos blockPos){
        for(UUID uuid : objetivesId){
            SheepEntity objetive = Utils.getEntityByUuid(world, uuid,blockPos,64,SheepEntity.class); // Magic number
            if(objetive!=null){
                objetive.kill();
            }
        }
        objetivesId.clear();
    }

    public void destroyAgent(World world,BlockPos blockPos){
        if(agentId!=null){
            ZombieEntity objetive = Utils.getEntityByUuid(world, agentId,blockPos,64,ZombieEntity.class); // Magic number
            if(objetive!=null){
                objetive.kill();
            }
        }

    }

    public static void updateObjetives(String objetivesString,AgentPlayerEntity entity,World world,BlockPos pos){
        String objetives[] = objetivesString.split(" ");
        
        for(int i = 0; i < objetives.length && objetives.length%2 == 0; i+=2){

            SheepEntity objetive = new SheepEntity(EntityType.SHEEP, world);

            objetive.setPosition(new Vec3d(Integer.parseInt(objetives[i])+0.5+pos.getX(),pos.getY(),Integer.parseInt(objetives[i+1])+0.5+pos.getZ()));
            objetive.setInvulnerable(true);
            objetive.setBaby(true);
            objetive.setAiDisabled(true);
            objetive.setSilent(true);

            world.spawnEntity(objetive);

            entity.objetivesId.add(objetive.getUuid());
        }
        
    }

    public AgentPlayerEntity(BlockPos pos, BlockState state) {
        super(Wandermod.AGENT_PLAYER_ENTITY, pos, state);
    }
    // Serialize the BlockEntity
    @Override
    public void writeNbt(NbtCompound nbt) {
        // Save the current value of the number to the nbt
        nbt.putInt("number", number);
        nbt.putIntArray("agentCoords", agentCoords);
        if(agentId!=null) {
            nbt.putUuid("agentEntity", agentId);
        }
        if(endId!=null) {
            nbt.putUuid("endEntity", endId);
        }
        
        for(int i = 0; i < objetivesId.size();i++){
            nbt.putUuid("objetive_"+i, objetivesId.get(i));
        }
        objetiveCount = objetivesId.size();
        nbt.putInt("objetiveCount",objetiveCount);
        
        super.writeNbt(nbt);
    }

    // Deserialize the BlockEntity
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
    
        number = nbt.getInt("number");
        agentCoords = nbt.getIntArray("agentCoords");
        agentId = nbt.getUuid("agentEntity");
        endId = nbt.getUuid("endEntity");
        objetiveCount = nbt.getInt("objetiveCount");

        objetivesId = new ArrayList<UUID>();
        
        for(int i = 0; i < objetiveCount;i++){
            UUID uuid = nbt.getUuid("objetive_"+i);
            objetivesId.add(uuid);
        }
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
                if(!hasMap){
                    String getMapSizeReply = sendMessage("getMapSize");
                    if(getMapSizeReply!=null){
                        String sizeString[] = getMapSizeReply.split(" ");
                        if(sizeString.length == 2){
                            int sizeX = Integer.parseInt(sizeString[0]);
                            int sizeY = Integer.parseInt(sizeString[1]);
                            String getMapReply = sendMessage("getMap");
                            if(getMapReply!=null){
                                buildMap(getMapReply, world, pos, sizeX, sizeY);
                                hasMap = true;
                            }
                        }
                    }
                }else{
                    String getAgentReply = sendMessage("getAgent");
                    Vec3d agentPos = Utils.stringToVectord(getAgentReply);
                    if(agentPos!=null){
                        updateAgent(world, pos, (int)agentPos.getX(), (int)agentPos.getZ(), entity);
                    }
                    // String getBufferReply = sendMessage("getBuffer");
                    // if(getAgentReply!=null
                    // &&!getAgentReply.equals("")
                    // &&!getAgentReply.equals("\n")){
                    //     Utils.sendMessageToAllPlayer(world,getBufferReply);
                    // }
                }
            }

            int power = world.getEmittedRedstonePower(pos, Direction.DOWN);
            if(power>0&&entity.lastPower==0){
                entity.activate(world, pos,null);
            }
            entity.lastPower = power;
            
            entity.markDirty();
        }
    }
}