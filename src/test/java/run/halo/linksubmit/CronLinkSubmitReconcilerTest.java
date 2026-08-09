package run.halo.linksubmit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import run.halo.linksubmit.extension.CronLinkSubmit;

class CronLinkSubmitReconcilerTest {

    @Test
    void enabledTaskIsNotSuspended() {
        var spec = new CronLinkSubmit.CronLinkSubmitSpec();
        spec.setSuspend(false);

        assertTrue(CronLinkSubmitReconciler.isEnabled(spec));
    }

    @Test
    void suspendedOrMissingTaskIsDisabled() {
        var spec = new CronLinkSubmit.CronLinkSubmitSpec();
        spec.setSuspend(true);

        assertFalse(CronLinkSubmitReconciler.isEnabled(spec));
        assertFalse(CronLinkSubmitReconciler.isEnabled(null));
    }
}
