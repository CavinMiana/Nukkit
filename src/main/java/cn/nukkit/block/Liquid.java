package cn.nukkit.block;

import cn.nukkit.entity.Entity;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.Vector3;

import java.util.Random;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class Liquid extends Transparent {
    private Vector3 temporalVector = null;

    public Liquid(int id) {
        super(id);
    }

    public Liquid(int id, int meta) {
        super(id, meta);
    }

    @Override
    public boolean hasEntityCollision() {
        return true;
    }

    @Override
    public boolean isBreakable(Item item) {
        return false;
    }

    @Override
    public boolean canBeReplaced() {
        return true;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    public int adjacentSources = 0;
    public boolean[] isOptimalFlowDirection = {false, false, false, false};
    public int[] flowinCost = {0, 0, 0, 0};

    public float getFluidHeightPercent() {
        float d = (float) this.meta;
        if (d >= 8) {
            d = 0;
        }

        return (d + 1) / 9f;
    }

    protected int getFlowDecay(Vector3 pos) {
        if (!(pos instanceof Block)) {
            pos = this.getLevel().getBlock(pos);
        }

        if (((Block) pos).getId() != this.getId()) {
            return -1;
        } else {
            return ((Block) pos).getDamage();
        }
    }

    protected int getEffectiveFlowDecay(Vector3 pos) {
        if (!(pos instanceof Block)) {
            pos = this.getLevel().getBlock(pos);
        }

        if (((Block) pos).getId() != this.getId()) {
            return -1;
        }

        int decay = ((Block) pos).getDamage();

        if (decay >= 8) {
            decay = 0;
        }

        return decay;
    }

    public Vector3 getFlowVector() {
        Vector3 vector = new Vector3(0, 0, 0);

        if (this.temporalVector == null) {
            this.temporalVector = new Vector3(0, 0, 0);
        }

        int decay = this.getEffectiveFlowDecay(this);

        for (int j = 0; j < 4; ++j) {

            double x = this.x;
            double y = this.y;
            double z = this.z;

            if (j == 0) {
                --x;
            } else if (j == 1) {
                ++x;
            } else if (j == 2) {
                --z;
            } else if (j == 3) {
                ++z;
            }
            Block sideBlock = this.getLevel().getBlock(this.temporalVector.setComponents(x, y, z));
            int blockDecay = this.getEffectiveFlowDecay(sideBlock);

            if (blockDecay < 0) {
                if (!sideBlock.canBeFlowedInto()) {
                    continue;
                }

                blockDecay = this.getEffectiveFlowDecay(this.getLevel().getBlock(this.temporalVector.setComponents(x, y - 1, z)));

                if (blockDecay >= 0) {
                    int realDecay = blockDecay - (decay - 8);
                    vector.x += (sideBlock.x - this.x) * realDecay;
                    vector.y += (sideBlock.y - this.y) * realDecay;
                    vector.z += (sideBlock.z - this.z) * realDecay;
                }

            } else {
                int realDecay = blockDecay - decay;
                vector.x += (sideBlock.x - this.x) * realDecay;
                vector.y += (sideBlock.y - this.y) * realDecay;
                vector.z += (sideBlock.z - this.z) * realDecay;
            }
        }

        if (this.getDamage() >= 8) {
            boolean falling = false;

            if (!this.getLevel().getBlock(this.temporalVector.setComponents(this.x, this.y, this.z - 1)).canBeFlowedInto()) {
                falling = true;
            } else if (!this.getLevel().getBlock(this.temporalVector.setComponents(this.x, this.y, this.z + 1)).canBeFlowedInto()) {
                falling = true;
            } else if (!this.getLevel().getBlock(this.temporalVector.setComponents(this.x - 1, this.y, this.z)).canBeFlowedInto()) {
                falling = true;
            } else if (!this.getLevel().getBlock(this.temporalVector.setComponents(this.x + 1, this.y, this.z)).canBeFlowedInto()) {
                falling = true;
            } else if (!this.getLevel().getBlock(this.temporalVector.setComponents(this.x, this.y + 1, this.z - 1)).canBeFlowedInto()) {
                falling = true;
            } else if (!this.getLevel().getBlock(this.temporalVector.setComponents(this.x, this.y + 1, this.z + 1)).canBeFlowedInto()) {
                falling = true;
            } else if (!this.getLevel().getBlock(this.temporalVector.setComponents(this.x - 1, this.y + 1, this.z)).canBeFlowedInto()) {
                falling = true;
            } else if (!this.getLevel().getBlock(this.temporalVector.setComponents(this.x + 1, this.y + 1, this.z)).canBeFlowedInto()) {
                falling = true;
            }

            if (falling) {
                vector = vector.normalize().add(0, -6, 0);
            }
        }

        return vector.normalize();
    }

    @Override
    public void addVelocityToEntity(Entity entity, Vector3 vector) {
        Vector3 flow = this.getFlowVector();
        vector.x += flow.x;
        vector.y += flow.y;
        vector.z += flow.z;
    }

    public int tickRate() {
        if (this instanceof Water) {
            return 5;
        } else if (this instanceof Lava) {
            return 30;
        }

        return 0;
    }

    @Override
    public int onUpdate(int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            this.checkForHarden();
            this.getLevel().scheduleUpdate(this, this.tickRate());
        } else if (type == Level.BLOCK_UPDATE_SCHEDULED) {
            if (this.temporalVector == null) {
                this.temporalVector = new Vector3(0, 0, 0);
            }

            int decay = this.getFlowDecay(this);
            int multiplier = this instanceof Lava ? 2 : 1;

            boolean flag = true;

            int smallestFlowDecay;
            if (decay > 0) {
                smallestFlowDecay = -100;
                this.adjacentSources = 0;
                smallestFlowDecay = this.getSmallestFlowDecay(this.level.getBlock(this.temporalVector.setComponents(this.x, this.y, this.z - 1)), smallestFlowDecay);
                smallestFlowDecay = this.getSmallestFlowDecay(this.level.getBlock(this.temporalVector.setComponents(this.x, this.y, this.z + 1)), smallestFlowDecay);
                smallestFlowDecay = this.getSmallestFlowDecay(this.level.getBlock(this.temporalVector.setComponents(this.x - 1, this.y, this.z)), smallestFlowDecay);
                smallestFlowDecay = this.getSmallestFlowDecay(this.level.getBlock(this.temporalVector.setComponents(this.x + 1, this.y, this.z)), smallestFlowDecay);

                int k = smallestFlowDecay + multiplier;

                if (k >= 8 || smallestFlowDecay < 0) {
                    k = -1;
                }

                int topFlowDecay;
                if ((topFlowDecay = this.getFlowDecay(this.level.getBlock(this.level.getBlock(this.temporalVector.setComponents(this.x, this.y + 1, this.z))))) >= 0) {
                    if (topFlowDecay >= 8) {
                        k = topFlowDecay;
                    } else {
                        k = topFlowDecay | 0x08;
                    }
                }


                if (this.adjacentSources >= 2 && this instanceof Water) {
                    Block bottomBlock = this.level.getBlock(this.level.getBlock(this.temporalVector.setComponents(this.x, this.y - 1, this.z)));
                    if (bottomBlock.isSolid()) {
                        k = 0;
                    } else if (bottomBlock instanceof Water && bottomBlock.getDamage() == 0) {
                        k = 0;
                    }
                }

                if (this instanceof Lava && decay < 8 && k < 8 && k > 1 && new Random().nextInt(4) != 0) {
                    k = decay;
                    flag = false;
                }

                if (k != decay) {
                    decay = k;
                    if (decay < 0) {
                        this.getLevel().setBlock(this, new Air(), true);
                    } else {
                        this.getLevel().setBlock(this, Block.get(this.id, decay), true);
                        this.getLevel().scheduleUpdate(this, this.tickRate());
                    }
                } else if (flag) {
                    //this.getLevel().scheduleUpdate(this, this.tickRate());
                    //this.updateFlow();
                }
            } else {
                //this.updateFlow();
            }

            Block bottomBlock = this.level.getBlock(this.temporalVector.setComponents(this.x, this.y - 1, this.z));

            if (bottomBlock.canBeFlowedInto() || bottomBlock instanceof Liquid) {
                if (this instanceof Lava && bottomBlock instanceof Water) {
                    this.getLevel().setBlock(bottomBlock, Block.get(Item.STONE), true);
                    return 0;
                }

                if (decay >= 8) {
                    this.getLevel().setBlock(bottomBlock, Block.get(this.id, decay), true);
                    this.getLevel().scheduleUpdate(bottomBlock, this.tickRate());
                } else {
                    this.getLevel().setBlock(bottomBlock, Block.get(this.id, decay + 8), true);
                    this.getLevel().scheduleUpdate(bottomBlock, this.tickRate());
                }
            } else if (decay >= 0 && (decay == 0 || !bottomBlock.canBeFlowedInto())) {
                boolean[] flags = this.getOptimalFlowDirections();

                int l = decay + multiplier;

                if (decay >= 8) {
                    l = 1;
                }

                if (l >= 8) {
                    this.checkForHarden();
                    return 0;
                }

                if (flags[0]) {
                    this.flowIntoBlock(this.level.getBlock(this.temporalVector.setComponents(this.x - 1, this.y, this.z)), l);
                }

                if (flags[1]) {
                    this.flowIntoBlock(this.level.getBlock(this.temporalVector.setComponents(this.x + 1, this.y, this.z)), l);
                }

                if (flags[2]) {
                    this.flowIntoBlock(this.level.getBlock(this.temporalVector.setComponents(this.x, this.y, this.z - 1)), l);
                }

                if (flags[3]) {
                    this.flowIntoBlock(this.level.getBlock(this.temporalVector.setComponents(this.x, this.y, this.z + 1)), l);
                }
            }

            this.checkForHarden();

        }

        return 0;
    }

    private void flowIntoBlock(Block block, int newFlowDecay) {
        if (block.canBeFlowedInto()) {
            if (block.getId() > 0) {
                this.getLevel().useBreakOn(block);
            }

            this.getLevel().setBlock(block, Block.get(this.getId(), newFlowDecay), true);
            this.getLevel().scheduleUpdate(block, this.tickRate());
        }
    }

    private int calculateFlowCost(Block block, int accumulatedCost, int previousDirection) {
        int cost = 1000;

        for (int j = 0; j < 4; ++j) {
            if (
                    (j == 0 && previousDirection == 1) ||
                            (j == 1 && previousDirection == 0) ||
                            (j == 2 && previousDirection == 3) ||
                            (j == 3 && previousDirection == 2)
                    ) {
                double x = block.x;
                double y = block.y;
                double z = block.z;

                if (j == 0) {
                    --x;
                } else if (j == 1) {
                    ++x;
                } else if (j == 2) {
                    --z;
                } else if (j == 3) {
                    ++z;
                }
                Block blockSide = this.getLevel().getBlock(this.temporalVector.setComponents(x, y, z));

                if (!blockSide.canBeFlowedInto() && !(blockSide instanceof Liquid)) {
                    continue;
                } else if (blockSide instanceof Liquid && blockSide.getDamage() == 0) {
                    continue;
                } else if (this.getLevel().getBlock(this.temporalVector.setComponents(x, y - 1, z)).canBeFlowedInto()) {
                    return accumulatedCost;
                }

                if (accumulatedCost >= 4) {
                    continue;
                }

                int realCost = this.calculateFlowCost(blockSide, accumulatedCost + 1, j);

                if (realCost < cost) {
                    cost = realCost;
                }
            }
        }

        return cost;
    }

    @Override
    public double getHardness() {
        return 100;
    }

    private boolean[] getOptimalFlowDirections() {
        if (this.temporalVector == null) {
            this.temporalVector = new Vector3(0, 0, 0);
        }

        for (int j = 0; j < 4; ++j) {
            this.flowinCost[j] = 1000;

            double x = this.x;
            double y = this.y;
            double z = this.z;

            if (j == 0) {
                --x;
            } else if (j == 1) {
                ++x;
            } else if (j == 2) {
                --z;
            } else if (j == 3) {
                ++z;
            }
            Block block = this.getLevel().getBlock(this.temporalVector.setComponents(x, y, z));

            if (!block.canBeFlowedInto() && !(block instanceof Liquid)) {
                continue;
            } else if (block instanceof Liquid && block.getDamage() == 0) {
                continue;
            } else if (this.getLevel().getBlock(this.temporalVector.setComponents(x, y - 1, z)).canBeFlowedInto()) {
                this.flowinCost[j] = 0;
            } else {
                this.flowinCost[j] = this.calculateFlowCost(block, 1, j);
            }
        }

        int minCost = this.flowinCost[0];

        for (int i = 1; i < 4; ++i) {
            if (this.flowinCost[i] < minCost) {
                minCost = this.flowinCost[i];
            }
        }

        for (int i = 0; i < 4; ++i) {
            this.isOptimalFlowDirection[i] = (this.flowinCost[i] == minCost);
        }

        return this.isOptimalFlowDirection;
    }

    private int getSmallestFlowDecay(Vector3 pos, int decay) {
        int blockDecay = this.getFlowDecay(pos);

        if (blockDecay < 0) {
            return decay;
        } else if (blockDecay == 0) {
            ++this.adjacentSources;
        } else if (blockDecay >= 8) {
            blockDecay = 0;
        }

        return (decay >= 0 && blockDecay >= decay) ? decay : blockDecay;
    }

    private void checkForHarden() {
        if (this instanceof Lava) {
            boolean colliding = false;
            for (int side = 0; side <= 5 && !colliding; ++side) {
                colliding = this.getSide(side) instanceof Water;
            }

            if (colliding) {
                if (this.getDamage() == 0) {
                    this.getLevel().setBlock(this, Block.get(Item.OBSIDIAN), true);
                } else if (this.getDamage() <= 4) {
                    this.getLevel().setBlock(this, Block.get(Item.COBBLESTONE), true);
                }
            }
        }
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return null;
    }

    @Override
    public int[][] getDrops(Item item) {
        return new int[0][];
    }
}
