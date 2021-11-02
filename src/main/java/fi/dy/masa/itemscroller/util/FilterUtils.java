package fi.dy.masa.itemscroller.util;


import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public class FilterUtils{
    private final static List<Direction> HORIZONTAL = List.of(Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH);
    private static BlockPos getTracedPos(MinecraftClient mc){
        return new BlockPos(mc.crosshairTarget.getPos());
    }
    private static BlockPos getFirstOutputComparator(BlockPos hopperPos, World world){
        Optional<BlockPos> comparatorPos = HORIZONTAL.stream().map(hopperPos::offset).
                filter(a->world.getBlockState(a).isOf(Blocks.COMPARATOR)
                        && a.offset(world.getBlockState(a).get(ComparatorBlock.FACING)).asLong() == hopperPos.asLong()).findAny();
        return comparatorPos.orElse(null);
    }
    private static Item getFirstCodedTarget(BlockPos comparatorPos, World world){
        BlockPos offsetPos;
        Block block = Blocks.AIR;
        boolean returnNext = false;
        for (int i = 1; i<4; i++){
            offsetPos = comparatorPos.offset(Direction.UP, i);
            BlockState blockState = world.getBlockState(offsetPos);
            if (returnNext){
                if (blockState.isAir()){
                    return block.asItem();
                }
                else {
                    return blockState.getBlock().asItem();
                }
            }
            if (!blockState.isAir()){
                if (blockState.isOf(Blocks.GLASS)){
                    block = Blocks.GLASS;
                    returnNext = true;
                }
                else {
                    return blockState.getBlock().asItem();
                }
            }
        }
        return block.asItem();
    }
    public static Item getTargetFromHopper(MinecraftClient mc){
        if (mc.world == null){
            return Items.AIR;
        }
        BlockPos hopperPos = getTracedPos(mc);
        if (!mc.world.getBlockState(hopperPos).isOf(Blocks.HOPPER)){
            return Items.AIR;
        }
        BlockPos comparatorPos = getFirstOutputComparator(hopperPos, mc.world);
        if (comparatorPos == null){
            return Items.AIR;
        }
        return getFirstCodedTarget(comparatorPos,mc.world);
    }
}