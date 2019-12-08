import java.awt.image.BufferedImage;

/**
 * This class has been copied from <br>
 * https://github.com/gabrielarchanjo/marvinproject/blob/master/marvinproject/dev/MarvinFramework/src/marvin/image/MarvinSegment.java <br>
 * It has been copied over in order to reduce dependency on the Marvin framework.
 *
 * @author Betalord
 */
public class MarvinSegment {

    public int width;
    public int height;
    int x1,
            x2,
            y1,
            y2;
    private int area;

    MarvinSegment(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.width = (x2 - x1) + 1;
        this.height = (y2 - y1) + 1;
        this.area = this.width * this.height;
    }

    // https://stackoverflow.com/questions/297762/find-known-sub-image-in-larger-image
    static synchronized MarvinSegment findSubimage(BufferedImage src, Cue cue, BrowserManager browserManager) {
        long timer = Misc.getTime();

        MarvinSegment seg;

        // Offset for do_not_share url missplacement of cues
        int x1, x2, y1, y2;

        // If the do_not_share url is available, cue detection is considering the correct offset
        if (browserManager.isDoNotShareUrl()) {
            x1 = cue.bounds != null && cue.bounds.x1 > 0 ? cue.bounds.x1 - 1 : 0;
            x2 = cue.bounds != null && cue.bounds.x2 > 0 ? cue.bounds.x2 - 1 : 0;
            y1 = cue.bounds != null && cue.bounds.y1 > 0 ? cue.bounds.y1 - 3 : 0;
            y2 = cue.bounds != null && cue.bounds.y2 > 0 ? cue.bounds.y2 - 3 : 0;
        } else {
            x1 = cue.bounds != null ? cue.bounds.x1 : 0;
            x2 = cue.bounds != null ? cue.bounds.x2 : 0;
            y1 = cue.bounds != null ? cue.bounds.y1 : 0;
            y2 = cue.bounds != null ? cue.bounds.y2 : 0;
        }

        seg = FindSubimage.findImage(src, cue.im, x1, y1, x2, y2);

        //source.drawRect(seg.x1, seg.y1, seg.x2-seg.x1, seg.y2-seg.y1, Color.blue);
        //MarvinImageIO.saveImage(source, "window_out.png");
        if (BHBot.settings.debugDetectionTimes) {
            BHBot.logger.info("cue detection time: " + (Misc.getTime() - timer) + "ms (" + cue.name + ") [" + (seg != null ? "true" : "false") + "]");
        }

        return seg;
    }

    /**
     * Will try (and retry) to detect cue from image until timeout is reached. May return null if cue has not been detected within given 'timeout' time. If 'timeout' is 0,
     * then it will attempt at cue detection only once and return the result immediately.
     */
    static synchronized MarvinSegment fromCue(Cue cue, int timeout, @SuppressWarnings("SameParameterValue") boolean game, BrowserManager browserManager) {
        long timer = Misc.getTime();
        MarvinSegment seg = findSubimage(browserManager.getImg(), cue, browserManager);

        while (seg == null) {
            if ((Misc.getTime() - timer) >= timeout)
                break;
            browserManager.readScreen(500, game);
            seg = findSubimage(browserManager.getImg(), cue, browserManager);
        }

        return seg;
    }

    static synchronized MarvinSegment fromCue(Cue cue, BrowserManager browserManager) {
        return fromCue(cue, 0, true, browserManager);
    }

    static synchronized MarvinSegment fromCue(Cue cue, int timeout, Bounds bounds, BrowserManager browserManager) {
        return fromCue(new Cue(cue, bounds), timeout, true, browserManager);
    }

    static synchronized MarvinSegment fromCue(Cue cue, int timeout, BrowserManager browserManager) {
        return fromCue(cue, timeout, true, browserManager);
    }

    // Cue detection based on String
    static synchronized MarvinSegment fromCue(String cueName, BrowserManager browserManager) {
        return fromCue(BrowserManager.cues.get(cueName), 0, true, browserManager);
    }

    static synchronized MarvinSegment fromCue(String cueName, int timeout, Bounds bounds, BrowserManager browserManager) {
        return fromCue(new Cue(BrowserManager.cues.get(cueName), bounds), timeout, true, browserManager);
    }

    static synchronized MarvinSegment fromCue(String cueName, int timeout, BrowserManager browserManager) {
        return fromCue(BrowserManager.cues.get(cueName), timeout, true, browserManager);
    }

    public String toString() {
        return "{x1:" + x1 + ", x2:" + x2 + ", y1:" + y1 + ", y2:" + y2 + ", width:" + width + ", height:" + height + ", area:" + area + "}";
    }

    int getX1() {
        return x1;
    }

    int getCenterX() {
        return (this.x1 + this.x2) / 2;
    }

    int getCenterY() {
        return (this.y1 + this.y2) / 2;
    }
}