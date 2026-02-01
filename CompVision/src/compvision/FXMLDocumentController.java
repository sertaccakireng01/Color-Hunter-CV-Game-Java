package compvision;

import java.io.ByteArrayInputStream;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class FXMLDocumentController {
    
    //Initializing UI Elements
    @FXML private Button btnStart;
    @FXML private ImageView currentFrame;

    private VideoCapture capture = new VideoCapture(); //object for getting Image from camera
    private ScheduledExecutorService timer; //Timer for Image flow at 30 FPS
    private boolean cameraActive = false; //Flag for if camera is active or not

    private String targetColorName = ""; //Target color name
    private int score = 0; //score
    private final int TOTAL_GAME_TIME = 30; //Total game time
    private long gameStartTime; //Game starting time (milliseconds)
    
    private boolean isFrozen = false; //Flag for if the screen is frozen
    private long freezeStartTime;//The time screen freezed
    private Mat frozenFrame = new Mat();//Keeping frozen Image as matrix 
    private boolean isFlash = false;//Flag for if the screen is flashed
    private long flashStartTime;//Starting time of flash

    /**
     * Toggles the camera stream on or off and initializes/shuts down the game timer.
     */
    @FXML
    protected void startCamera(ActionEvent event) {
        if (!this.cameraActive) {//checks if camera is not active
            this.capture.open(0);//then opens main (0) camera on device
            if (this.capture.isOpened()) {//if capture object is opened
                this.cameraActive = true;//Make camera is active
                resetGame(); //reset game and start
                //process frames
                Runnable frameGrabber = () -> {
                    Mat frame = grabFrame(); //takes raw frame
                    processGameLogic(frame);//process game logic on frame
                    Image imageToShow = mat2Image(frame); //turn OpenCV matrix into JavaFX Image
                    //Update UI (used runLater for Thread safety
                    Platform.runLater(() -> currentFrame.setImage(imageToShow));
                };
                //Starting timer (1 frameGrabber per 33 milliseconds = approximately 30FPS)
                this.timer = Executors.newSingleThreadScheduledExecutor();
                this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
                this.btnStart.setText("Stop Game");
            }
        } else { // If cam is already active, do stopping actions of cam
            this.cameraActive = false;
            this.btnStart.setText("Start Game");
            this.stopAcquisition();
        }
    }

    /**
     * Resets score, game timer, and selects the first target color.
     */
    private void resetGame() {
        this.score = 0;
        this.isFrozen = false;
        this.isFlash = false;
        this.gameStartTime = System.currentTimeMillis();//
        setNewTarget();
    }

    /**
     * Manages the main game states: Game Over, Success Freeze, Flash Notification, and Real-time Analysis.
     */
    private void processGameLogic(Mat frame) {
        if (frame.empty()) return;
        //Time control: Total time - elapssed time = remainingtime
        long elapsed = (System.currentTimeMillis() - gameStartTime) / 1000;
        int remainingTime = Math.max(0, TOTAL_GAME_TIME - (int)elapsed);

        if (remainingTime <= 0 && !isFrozen && !isFlash) {
            drawGameOverScreen(frame);
            return;
        }

        if (isFrozen) {
            frozenFrame.copyTo(frame);
            handleSuccessFreeze(frame);
            return;
        }

        if (isFlash) {
            handleTargetFlash(frame);
            return;
        }

        runColorAnalysis(frame, remainingTime);
    }

    /**
     * Renders the final score and "Game Over" overlay on the current frame.
     */
    private void drawGameOverScreen(Mat frame) {
        Imgproc.rectangle(frame, new Point(0, 0), new Point(frame.width(), frame.height()), new Scalar(0, 0, 0), -1);
        Imgproc.putText(frame, "GAME OVER!", new Point(frame.width()/6, frame.height()/2), 
                Core.FONT_HERSHEY_SIMPLEX, 3.0, new Scalar(0, 255, 255), 5);
        Imgproc.putText(frame, "TOTAL SCORE: " + score, new Point(frame.width()/6, frame.height()/2 + 80), 
                Core.FONT_HERSHEY_SIMPLEX, 1.5, new Scalar(255, 255, 255), 3);
    }

    /**
     * Keeps the successful frame static for 3 seconds and counts down to the next target.
     */
    private void handleSuccessFreeze(Mat frame) {
        long elapsedFreeze = (System.currentTimeMillis() - freezeStartTime) / 1000;
        int freezeCountdown = 3 - (int)elapsedFreeze;

        Imgproc.putText(frame, "COLOR ACCEPTED!", new Point(50, frame.height() / 2 - 40), 
                Core.FONT_HERSHEY_SIMPLEX, 1.8, new Scalar(0, 255, 0), 4);
        Imgproc.putText(frame, "NEXT TARGET IN: " + freezeCountdown, new Point(50, frame.height() / 2 + 50), 
                Core.FONT_HERSHEY_SIMPLEX, 1.5, new Scalar(255, 255, 255), 3);

        if (freezeCountdown <= 0) {
            isFrozen = false;
            setNewTarget();
            isFlash = true;
            flashStartTime = System.currentTimeMillis();
            frozenFrame.release();
        }
    }

    /**
     * Renders a quick 250ms notification screen displaying the name of the next target color.
     */
    private void handleTargetFlash(Mat frame) {
        long elapsedFlash = System.currentTimeMillis() - flashStartTime;
        frame.setTo(new Scalar(30, 30, 30));
        
        Imgproc.putText(frame, "NEXT TASK:", new Point(frame.width()/4, frame.height()/2 - 100), 
                Core.FONT_HERSHEY_SIMPLEX, 1.5, new Scalar(255, 255, 255), 2);
        
        Imgproc.putText(frame, targetColorName, new Point(frame.width()/5, frame.height()/2 + 60), 
                Core.FONT_HERSHEY_SIMPLEX, 4.5, new Scalar(255, 255, 255), 10);

        if (elapsedFlash > 250) { 
            isFlash = false;
            gameStartTime += 3250; 
        }
    }

    /**
     * Crops the center ROI, calculates the mean color, and computes accuracy based on Euclidean distance.
     */
    private void runColorAnalysis(Mat frame, int remainingTime) {
        int cx = frame.width() / 2;
        int cy = frame.height() / 2;
        int boxSize = 50;

        Imgproc.rectangle(frame, new Point(cx - boxSize, cy - boxSize), 
                new Point(cx + boxSize, cy + boxSize), new Scalar(0, 255, 255), 2);

        Mat roi = frame.submat(cy - boxSize, cy + boxSize, cx - boxSize, cx + boxSize);
        Mat roiRGB = new Mat();
        Imgproc.cvtColor(roi, roiRGB, Imgproc.COLOR_BGR2RGB);//turns BGR to RGB
        Scalar meanColor = Core.mean(roiRGB);

        Scalar targetRGB = getTargetRGBValues();
        double distance = calculateEuclideanDistance(meanColor, targetRGB);
        boolean isCorrectTone = checkColorDominance(meanColor, targetColorName);
        double accuracy = Math.max(0, Math.min(100, 100.0 * (1.0 - (distance / 350.0))));

        updateHUD(frame, remainingTime, accuracy, isCorrectTone);

        if (accuracy > 65.0 && isCorrectTone) {
            score += 10;
            triggerSuccess(frame, cx, cy, boxSize);
        }

        roi.release();
        roiRGB.release();
    }

    /**
     * Returns the ideal RGB Scalar values for the currently active target color name.
     */
    private Scalar getTargetRGBValues() {
        if (targetColorName.equals("BLUE")) return new Scalar(30, 30, 220);
        if (targetColorName.equals("RED")) return new Scalar(220, 30, 30);
        return new Scalar(30, 220, 30);
    }

    /**
     * Updates the Head-Up Display (HUD) with target info, current score, timer, and accuracy percentage.
     */
    private void updateHUD(Mat frame, int remainingTime, double accuracy, boolean isCorrectTone) {
        Imgproc.putText(frame, "TARGET: " + targetColorName, new Point(30, 65), 1, 2.2, new Scalar(255, 255, 255), 3);
        Imgproc.putText(frame, "SCORE: " + score, new Point(30, 120), 1, 1.8, new Scalar(0, 255, 0), 2);
        Imgproc.putText(frame, "TIME: " + remainingTime, new Point(frame.width() - 230, 65), 1, 2.0, new Scalar(0, 0, 255), 3);

        Scalar accuracyColor = (accuracy > 65.0 && isCorrectTone) ? new Scalar(0, 255, 0) : new Scalar(0, 0, 255);
        Imgproc.putText(frame, String.format("Accuracy: %%%.1f", accuracy), new Point(30, frame.height() - 40), 1, 1.4, accuracyColor, 2);
    }

    /**
     * Captures the current frame as a static success image and initializes the freeze state.
     */
    private void triggerSuccess(Mat frame, int cx, int cy, int boxSize) {
        isFrozen = true;
        freezeStartTime = System.currentTimeMillis();
        Imgproc.putText(frame, "EXCELLENT!", new Point(cx - 110, cy + 110), 1, 2.2, new Scalar(0, 255, 0), 4);
        Imgproc.rectangle(frame, new Point(cx-boxSize, cy-boxSize), new Point(cx+boxSize, cy+boxSize), new Scalar(0, 255, 0), 5);
        frame.copyTo(frozenFrame);//use reference for OpenCV matrix
    }

    /**
     * Computes the mathematical distance between two colors using the 3D Euclidean distance formula.
     */
    private double calculateEuclideanDistance(Scalar s1, Scalar s2) {
        return Math.sqrt(Math.pow(s1.val[0]-s2.val[0],2) + Math.pow(s1.val[1]-s2.val[1],2) + Math.pow(s1.val[2]-s2.val[2],2));
    }

    /**
     * Randomly picks a new color name from the available set (RED, GREEN, BLUE).
     */
    private void setNewTarget() {
        String[] colors = {"RED", "GREEN", "BLUE"};
        targetColorName = colors[new Random().nextInt(3)];
    }

    /**
     * Verifies if the selected RGB channel is numerically dominant compared to others to ensure color purity.
     */
    private boolean checkColorDominance(Scalar c, String target) {
        double r = c.val[0], g = c.val[1], b = c.val[2], tolerance = 25.0;
        if (target.equals("BLUE")) return (b > g + tolerance && b > r + tolerance);
        if (target.equals("RED")) return (r > g + tolerance && r > b + tolerance);
        if (target.equals("GREEN")) return (g > r + tolerance && g > b + tolerance);
        return false;
    }

    /**
     * Reads a raw frame from the camera and applies a horizontal flip for a mirror effect.
     */
    private Mat grabFrame() {//does mirror effect for cam
        Mat frame = new Mat();
        if (this.capture.isOpened()) {
            this.capture.read(frame);
            if (!frame.empty()) Core.flip(frame, frame, 1);
        }
        return frame;
    }

    /**
     * Shuts down the executor timer and releases the VideoCapture hardware.
     */
    public void stopAcquisition() {
        if (this.timer != null && !this.timer.isShutdown()) this.timer.shutdown();
        if (this.capture.isOpened()) this.capture.release();
    }

    /**
     * Converts an OpenCV Mat into a JavaFX Image by encoding it to a PNG byte buffer.
     */
    private Image mat2Image(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }
}