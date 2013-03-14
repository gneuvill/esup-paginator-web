package epw.utils;

import fj.Unit;
import fj.control.parallel.ParModule;
import fj.control.parallel.Strategy;

import java.util.concurrent.Executors;

import static fj.control.parallel.ParModule.parModule;

public class ParallelModule {

    public static final ParModule parMod = parModule(
            Strategy.<Unit>executorStrategy(Executors.newFixedThreadPool(50)));


}
