package com.wandermod;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class Utils {
    static public void sendMessageToAllPlayer(World world, String msg){
        for(PlayerEntity player : world.getPlayers()){
            player.sendMessage(Text.literal(msg),false);
        }
    }

    static public Entity getEntityByUuid(World world,UUID uuid,BlockPos blockPos, int radius){
        Box box = new Box(blockPos).expand(radius);
        List<Entity> entities = world.getEntitiesByClass(Entity.class,box ,entity -> entity.isAlive());
        for(Entity entity : entities){
            if(entity.getUuid().equals(uuid)){
                return entity;
            }
        }
        return null;
    }

    static public <T extends Entity> T getEntityByUuid(World world,UUID uuid,BlockPos blockPos, int radius,Class<T> entityClass){
        Box box = new Box(blockPos).expand(radius);
        List<T> entities = world.getEntitiesByClass(entityClass,box ,entity -> entity.isAlive());
        for(T entity : entities){
            if(entity.getUuid().equals(uuid)){
                return entity;
            }
        }
        return null;
    }

    static public Vec3d stringToVectord(String pos){
        if(pos!=null){
            String stringPos[] = pos.split(" ");
            try{
                if(stringPos.length==2){
                    double x = (double)Integer.parseInt(stringPos[0]);
                    double y = (double)Integer.parseInt(stringPos[1]);
                    return new Vec3d(x,0,y);
                }
            }catch(NumberFormatException e){}
        }

        return null;
    }
}
