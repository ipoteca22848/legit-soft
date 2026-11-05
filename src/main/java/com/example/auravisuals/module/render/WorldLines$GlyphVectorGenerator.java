package com.example.auravisuals.module.render;

public class WorldLines$GlyphVectorGenerator {
    public WorldLines outer;
    public final double[] nodeX = new double[20];
    public final double[] nodeY = new double[20];
    public final double[] nodeZ = new double[20];
    public int nodeCount = 0;
    public int drawnNodes = 1;
    public double segProgress = 0.0;
    public final double segSpeed;
    public byte dirX;
    public byte dirY;
    public byte dirZ;
    public boolean fadingOut = false;
    public long fadeStart = -1L;
    public int tailNode = 0;
    public double tailProgress = 0.0;
    public final double seed;

    public WorldLines$GlyphVectorGenerator(WorldLines outer, double spawnX, double spawnY, double spawnZ, int maxSteps, double seed) {
        this.outer = outer;
        this.seed = seed;
        this.nodeX[0] = spawnX;
        this.nodeY[0] = spawnY;
        this.nodeZ[0] = spawnZ;
        this.nodeCount = 1;
        this.segSpeed = 0.06 * this.outer.speed.getValue();
        this.randomAxisDir();
        this.buildPath(maxSteps);
    }

    public void buildPath(int steps) {
        for (int s = 0; s < steps && this.nodeCount < 20; s++) {
            this.nextDir();
            int len = this.outer.random.nextInt(3) + 1;
            this.nodeX[this.nodeCount] = this.nodeX[this.nodeCount - 1] + this.dirX * len;
            this.nodeY[this.nodeCount] = this.nodeY[this.nodeCount - 1] + this.dirY * len;
            this.nodeZ[this.nodeCount] = this.nodeZ[this.nodeCount - 1] + this.dirZ * len;
            this.nodeCount++;
        }
    }

    public void randomAxisDir() {
        int idx = this.outer.random.nextInt(WorldLines.AXES.length);
        this.dirX = (byte)WorldLines.AXES[idx][0];
        this.dirY = (byte)WorldLines.AXES[idx][1];
        this.dirZ = (byte)WorldLines.AXES[idx][2];
    }

    public void nextDir() {
        int count = 0;

        for (int[] axis : WorldLines.AXES) {
            if (axis[0] != -this.dirX || axis[1] != -this.dirY || axis[2] != -this.dirZ) {
                count++;
            }
        }

        int pick = this.outer.random.nextInt(count);

        for (int[] axis : WorldLines.AXES) {
            if ((axis[0] != -this.dirX || axis[1] != -this.dirY || axis[2] != -this.dirZ) && pick-- == 0) {
                this.dirX = (byte)axis[0];
                this.dirY = (byte)axis[1];
                this.dirZ = (byte)axis[2];
                return;
            }
        }
    }

    public void update() {
        if (!this.fadingOut) {
            if (this.drawnNodes < this.nodeCount) {
                this.segProgress = this.segProgress + this.segSpeed;
                if (this.segProgress >= 1.0) {
                    this.segProgress--;
                    this.drawnNodes++;
                }
            } else {
                this.fadingOut = true;
                this.fadeStart = System.currentTimeMillis();
            }

            if (this.drawnNodes - this.tailNode > 14) {
                this.tailProgress = this.tailProgress + this.segSpeed;
                if (this.tailProgress >= 1.0) {
                    this.tailProgress--;
                    this.tailNode++;
                }
            }
        }
    }

    public int fillPoints(double[] xs, double[] ys, double[] zs, float tickDelta) {
        if (this.nodeCount < 2) {
            return 0;
        }

        int renderTailNode = Math.min(this.tailNode, this.nodeCount - 1);
        int renderDrawnNodes = Math.min(this.drawnNodes, this.nodeCount - 1);
        boolean tailMoving = !this.fadingOut && this.drawnNodes - this.tailNode > 14;
        double renderTailProgress = tailMoving ? Math.min(1.0, this.tailProgress + this.segSpeed * tickDelta) : this.tailProgress;
        double renderSegProgress = this.fadingOut ? 1.0 : Math.min(1.0, this.segProgress + this.segSpeed * tickDelta);
        int out = 0;
        if (renderTailNode < this.nodeCount - 1) {
            double t = renderTailProgress;
            xs[out] = this.nodeX[renderTailNode] + (this.nodeX[renderTailNode + 1] - this.nodeX[renderTailNode]) * t;
            ys[out] = this.nodeY[renderTailNode] + (this.nodeY[renderTailNode + 1] - this.nodeY[renderTailNode]) * t;
            zs[out] = this.nodeZ[renderTailNode] + (this.nodeZ[renderTailNode + 1] - this.nodeZ[renderTailNode]) * t;
        } else {
            xs[out] = this.nodeX[renderTailNode];
            ys[out] = this.nodeY[renderTailNode];
            zs[out] = this.nodeZ[renderTailNode];
        }

        out++;

        for (int i = renderTailNode + 1; i <= renderDrawnNodes && out < xs.length; i++) {
            xs[out] = this.nodeX[i];
            ys[out] = this.nodeY[i];
            zs[out] = this.nodeZ[i];
            out++;
        }

        if (this.drawnNodes < this.nodeCount && out > 0) {
            int dn = renderDrawnNodes;
            double t = renderSegProgress;
            xs[--out] = this.nodeX[dn - 1] + (this.nodeX[dn] - this.nodeX[dn - 1]) * t;
            ys[out] = this.nodeY[dn - 1] + (this.nodeY[dn] - this.nodeY[dn - 1]) * t;
            zs[out] = this.nodeZ[dn - 1] + (this.nodeZ[dn] - this.nodeZ[dn - 1]) * t;
            out++;
        }

        return out;
    }

    public double getFadeOut() {
        if (!this.fadingOut) {
            return 1.0;
        }

        float t = (float)(System.currentTimeMillis() - this.fadeStart) / 700.0F;
        return t >= 1.0F ? 0.0 : 1.0 - t * t * t;
    }

    public boolean isExpired() {
        return this.fadingOut && this.getFadeOut() <= 0.0;
    }
}
