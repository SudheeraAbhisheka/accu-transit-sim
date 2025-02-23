package org.example.multidisplaycore.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SimulationServiceTest {
    private final int NO_OF_TEST_DATA = 6;
    private final CountDownLatch latch = new CountDownLatch(NO_OF_TEST_DATA);

    @Autowired
    private ServiceCore simulationService;

    @ParameterizedTest
    @CsvSource({
            "'1', 90.0, 180.0, 1",
            "'2', 60.0, 150.0, 2",
            "'3', 30.0, 120.0, 3",
            "'1R', 75.0, 150.0, 4",
            "'2R', 105.0, 195.0, 5",
            "'3R', 135.0, 450.0, 6"

    })
    public void testSimulator(String routeId, double lowestSpeed, double highestSpeed, long startAfter) throws InterruptedException {
        simulationService.setLatch(latch);
        simulationService.simulator(routeId, lowestSpeed, highestSpeed, startAfter);
    }

    @AfterAll
    void awaitLatch() throws InterruptedException {
        boolean finished = latch.await(11, TimeUnit.MINUTES);
        assertTrue(finished, "The simulation did not complete in time");
    }
}
