package th.in.spksoft.taxi;

import com.google.android.gms.maps.model.LatLng;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void test_meter_distance_from_two_latlng() throws Exception {
        double distance = MapsActivity.getMeterFromLatLng(new LatLng(13.651679, 100.4869247), new LatLng(13.6472733, 100.4943752));
        System.out.println(distance);
        assertEquals((int)distance, 942);
    }
    @Test
    public void test_price_from_meter() throws Exception {
        double price = MapsActivity.calculatePriceFromMeter(942);
        System.out.println(price);
        assertEquals((int)price, 35);
    }
}