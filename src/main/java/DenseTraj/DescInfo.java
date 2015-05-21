package DenseTraj;

/**
 * Created by Tom.fu on 24/2/2015.
 */
public class DescInfo {
    int nBins, nxCells, nyCells, ntCells, dim, height, width;
    boolean isHof;

    public DescInfo(int nBins, boolean isHof, int size, int nxy_cell, int nt_cell) {
        this.nBins = nBins;
        this.isHof = isHof;
        this.nxCells = nxy_cell;
        this.nyCells = nxy_cell;
        this.ntCells = nt_cell;
        this.dim = nBins * nxy_cell * nxy_cell;
        this.height = size;
        this.width = size;
    }
}
