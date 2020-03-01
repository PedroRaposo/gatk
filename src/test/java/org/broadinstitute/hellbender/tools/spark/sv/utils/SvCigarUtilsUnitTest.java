package org.broadinstitute.hellbender.tools.spark.sv.utils;

import htsjdk.samtools.Cigar;
import htsjdk.samtools.CigarElement;
import htsjdk.samtools.TextCigarCodec;
import org.broadinstitute.hellbender.GATKBaseTest;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;


public class SvCigarUtilsUnitTest extends GATKBaseTest {

    @Test(groups = "sv")
    public void testClippingArithmetic() {
        Cigar cigar = TextCigarCodec.decode("100M51S");
        Assert.assertEquals(SvCigarUtils.getTotalHardClipping(cigar), 0);
        Assert.assertEquals(SvCigarUtils.getNumClippedBases(true, cigar), 0);
        Assert.assertEquals(SvCigarUtils.getNumClippedBases(false, cigar), 51);

        cigar = TextCigarCodec.decode("51S100M");
        Assert.assertEquals(SvCigarUtils.getTotalHardClipping(cigar), 0);
        Assert.assertEquals(SvCigarUtils.getNumClippedBases(true, cigar), 51);
        Assert.assertEquals(SvCigarUtils.getNumClippedBases(false, cigar), 0);

        cigar = TextCigarCodec.decode("100M51H");
        Assert.assertEquals(SvCigarUtils.getTotalHardClipping(cigar), 51);
        Assert.assertEquals(SvCigarUtils.getNumClippedBases(true, cigar), 0);
        Assert.assertEquals(SvCigarUtils.getNumClippedBases(false, cigar), 51);

        cigar = TextCigarCodec.decode("51H100M");
        Assert.assertEquals(SvCigarUtils.getTotalHardClipping(cigar), 51);
        Assert.assertEquals(SvCigarUtils.getNumClippedBases(true, cigar), 51);
        Assert.assertEquals(SvCigarUtils.getNumClippedBases(false, cigar), 0);

        cigar = TextCigarCodec.decode("12H12S101M13S13H");
        Assert.assertEquals(SvCigarUtils.getTotalHardClipping(cigar), 25);
        Assert.assertEquals(SvCigarUtils.getNumClippedBases(true, cigar), 24);
        Assert.assertEquals(SvCigarUtils.getNumClippedBases(false, cigar), 26);
    }

    @Test(groups = "sv")
    public void testGetNumClippingBases_hardAndSoftSeparately() {
        List<CigarElement> cigarElements = TextCigarCodec.decode("10H20S30M40D50M60S70H").getCigarElements();
        Assert.assertEquals(SvCigarUtils.getNumSoftClippingBases(true, cigarElements), 20);
        Assert.assertEquals(SvCigarUtils.getNumSoftClippingBases(false, cigarElements), 60);
    }

    @DataProvider(name = "refWalkDistanceTestDataException")
    private Object[][] createRefWalkDistanceTestDataException() {
        final List<Object[]> data = new ArrayList<>(20);
        data.add(new Object[]{TextCigarCodec.decode("50M10N101M"), 41, 10, 0});
        data.add(new Object[]{TextCigarCodec.decode("50M10P101M"), 41, 10, 0});
        final Cigar cigar = TextCigarCodec.decode("35H40S10M20I25M30D50M55S60H");
        data.add(new Object[]{cigar, -1, 10, 0});
        data.add(new Object[]{cigar, 0, 10, 0});
        data.add(new Object[]{cigar, 41, -1, 0});
        data.add(new Object[]{cigar, 41, 0, 0});
        data.add(new Object[]{cigar, 1, 201, 0});
        return data.toArray(new Object[data.size()][]);
    }

    @Test(dataProvider = "refWalkDistanceTestDataException", groups = "sv", expectedExceptions = IllegalArgumentException.class)
    public void testRefWalkDistanceException(final Cigar cigar, final int startInclusive, final int distance,
                                             final int expectedRefDist) {
        Assert.assertEquals(SvCigarUtils.computeAssociatedDistOnRef(cigar, startInclusive, distance), expectedRefDist);
    }

    @DataProvider(name = "refWalkDistanceTestData")
    private Object[][] createRefWalkDistanceTestData() {
        final List<Object[]> data = new ArrayList<>(20);
        final Cigar cigar = TextCigarCodec.decode("35H40S10M20I25M30D50M55S60H");

        data.add(new Object[]{cigar, 1, 40, 0});
        data.add(new Object[]{cigar, 1, 45, 5});
        data.add(new Object[]{cigar, 41, 10, 10});
        data.add(new Object[]{cigar, 41, 30, 10});
        data.add(new Object[]{cigar, 41, 25, 10});
        data.add(new Object[]{cigar, 41, 35, 15});
        data.add(new Object[]{cigar, 41, 56, 66});
        data.add(new Object[]{cigar, 41, 110, 115});
        data.add(new Object[]{cigar, 1, 200, 115});
        data.add(new Object[]{cigar, 61, 10, 0});
        data.add(new Object[]{cigar, 61, 15, 5});
        data.add(new Object[]{cigar, 45, 6, 6});
        data.add(new Object[]{cigar, 45, 16, 6});
        data.add(new Object[]{cigar, 45, 26, 6});
        data.add(new Object[]{cigar, 45, 27, 7});
        data.add(new Object[]{cigar, 45, 51, 31});
        data.add(new Object[]{cigar, 45, 52, 62});

        data.add(new Object[]{TextCigarCodec.decode("10M1I5M"), 5, 10, 9});
        data.add(new Object[]{TextCigarCodec.decode("10M1D5M"), 5, 10, 11});

        return data.toArray(new Object[data.size()][]);
    }

    @Test(dataProvider = "refWalkDistanceTestData", groups = "sv")
    public void testRefWalkDistance(final Cigar cigar, final int startInclusive, final int distance, final int expectedRefDist) {
        Assert.assertEquals(SvCigarUtils.computeAssociatedDistOnRef(cigar, startInclusive, distance), expectedRefDist);
    }

    @DataProvider(name = "readWalkDistanceTestDataException")
    private Object[][] createReadWalkDistanceTestDataException() {
        final List<Object[]> data = new ArrayList<>(20);

        data.add(new Object[]{TextCigarCodec.decode("50M10N101M"), 41, 10, false, 0});
        data.add(new Object[]{TextCigarCodec.decode("50M10P101M"), 41, 10, false, 0});

        final Cigar cigar = TextCigarCodec.decode("35H40S10M20I25M30D50M55S60H");
        data.add(new Object[]{cigar, -1, 10, false, 0});
        data.add(new Object[]{cigar, 0, 10, false, 0});
        data.add(new Object[]{cigar, 41, -1, false, 0});
        data.add(new Object[]{cigar, 41, 0, false, 0});

        data.add(new Object[]{cigar, 1, 116, false, 0});
        data.add(new Object[]{cigar, 200, 116, false, 0});

        data.add(new Object[]{cigar, 96, 51, false, 0});
        data.add(new Object[]{cigar, 145, 2, false, 0});
        data.add(new Object[]{cigar, 146, 1, false, 0});

        data.add(new Object[]{cigar, 96, 67, true, 0});
        data.add(new Object[]{cigar, 40, 1, true, 0});
        data.add(new Object[]{cigar, 41, 2, true, 0});

        return data.toArray(new Object[data.size()][]);
    }

    @Test(dataProvider = "readWalkDistanceTestDataException", groups = "sv", expectedExceptions = IllegalArgumentException.class)
    public void testReadWalkDistanceTestDataException(final Cigar cigar, final int startInclusive, final int refWalkDist,
                                                      final boolean walkBackwards, final int expectedReadWalkDist) {
        Assert.assertEquals(SvCigarUtils.computeAssociatedDistOnRead(cigar, startInclusive, refWalkDist, walkBackwards), expectedReadWalkDist);
    }

    @DataProvider(name = "readWalkDistanceTestData")
    private Object[][] createReadWalkDistanceTestData() {

        final List<Object[]> data = new ArrayList<>(20);
        final Cigar cigar = TextCigarCodec.decode("35H40S10M20I25M30D50M55S60H");

        data.add(new Object[]{cigar, 1, 5, false, 45});
        data.add(new Object[]{cigar, 1, 10, false, 50});
        data.add(new Object[]{cigar, 1, 16, false, 76});
        data.add(new Object[]{cigar, 1, 64, false, 95});
        data.add(new Object[]{cigar, 1, 66, false, 96});
        data.add(new Object[]{cigar, 11, 5, false, 35});
        data.add(new Object[]{cigar, 41, 64, false, 55});

        data.add(new Object[]{cigar, 146, 1, true, 2});

        data.add(new Object[]{cigar, 181, 10, true, 46});
        data.add(new Object[]{cigar, 181, 50, true, 86});
        data.add(new Object[]{cigar, 181, 51, true, 86});
        data.add(new Object[]{cigar, 181, 80, true, 86});
        data.add(new Object[]{cigar, 181, 105, true, 111});
        data.add(new Object[]{cigar, 181, 106, true, 132});
        data.add(new Object[]{cigar, 181, 115, true, 141});

        return data.toArray(new Object[data.size()][]);
    }

    @Test(dataProvider = "readWalkDistanceTestData", groups = "sv")
    public void testReadWalkDistanceTestData(final Cigar cigar, final int startInclusiveOnRead, final int refWalkDist,
                                             final boolean walkBackwards, final int expectedReadWalkDist) {
        Assert.assertEquals(SvCigarUtils.computeAssociatedDistOnRead(cigar, startInclusiveOnRead, refWalkDist, walkBackwards), expectedReadWalkDist);
    }
}
