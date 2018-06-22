package com.example.eppsna01.tictactoe2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
/*import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;*/
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class MainActivity extends Activity
{
    private MediaPlayer music;

    // The following are used for the shake detection
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeSensor mShakeDetector;
    private GameView game;

    private static void displayInfo(Context context, String mssg)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(mssg).setPositiveButton("Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(new MainMenu(this));

        music = MediaPlayer.create(this, R.raw.music);
        music.start();

        final MainActivity ma = this;

        // ShakeDetector initialization
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mShakeDetector = new ShakeSensor(new ShakeSensor.ShakeListener() {
            @Override
            public void onShake() {
                /*
                 * The following method, "handleShakeEvent(count):" is a stub/
                 * method you would use to setup whatever you want done once the
                 * device has been shook.
                 */
                displayInfo(ma, "onShake()");

                if (game != null)
                    game.onShake();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Add the following line to register the Session Manager Listener onResume
        mSensorManager.registerListener(mShakeDetector, mAccelerometer,	SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        // Add the following line to unregister the Sensor Manager onPause
        mSensorManager.unregisterListener(mShakeDetector);
        super.onPause();
    }

    public static int randInt(int lo, int hi)
    {
        int r = new Random().nextInt();

        return ((r >= 0)?(r):(-1 * r)) % (hi - lo + 1) + lo;
    }

    private static class Pair<E>
    {
        public E first, second;

        public Pair(E x, E y)
        {
            first = x;
            second = y;
        }

        public Pair()
        {
            first = null;
            second = null;
        }
    }

    private static class MainMenu extends View
    {
        long start;
        MainActivity parent;

        public MainMenu(final Context context)
        {
            super(context);

            parent = (MainActivity) context;

            start = -1;
        }

        @Override
        public boolean onTouchEvent(MotionEvent me)
        {
            parent.setContentView(new GameView(parent));

            return true;
        }

        @Override
        public void onDraw(Canvas cvs)
        {
            if (start == -1)
                start = System.currentTimeMillis();

            Paint titlePaint = new Paint();
            titlePaint.setTextSize((float) (getWidth() * 0.15));
            titlePaint.setColor(Color.BLACK);

            float twidth = titlePaint.measureText("Tic-Tac-Toe");

            cvs.drawText("Tic-Tac-Toe", (getWidth() - twidth) / 2, (float) (getHeight() * 0.35), titlePaint);

            Paint copyrightPaint = new Paint();
            copyrightPaint.setTextSize((float) (getWidth() * 0.05));
            copyrightPaint.setColor(Color.BLACK);

            String copyright = "(c) Nathanael Epps 2018";
            Rect cbounds = new Rect();
            copyrightPaint.getTextBounds(copyright, 0, copyright.length(), cbounds);
            cvs.drawText(copyright, (getWidth() - cbounds.width()) / 2, getHeight() - cbounds.height(), copyrightPaint);

            Paint noticePaint = new Paint();
            noticePaint.setTextSize((float) (getWidth() * 0.075));
            noticePaint.setColor(Color.BLUE);

            float nwidth = noticePaint.measureText("Tap to play!");

            cvs.drawText("Tap to play!", (getWidth() - nwidth) / 2, (float) (getHeight() * 0.6), noticePaint);
        }
    }

    private static class ShakeSensor implements SensorEventListener
    {
        private static final float SHAKE_THRESHOLD_GRAVITY = 2.7F;
        private static final int SHAKE_SLOP_TIME_MS = 500;
        private static final int SHAKE_COUNT_RESET_TIME_MS = 3000;

        private long mShakeTimestamp;
        private int mShakeCount;
        private ShakeListener mlistener;

        public static interface ShakeListener
        {
            public void onShake();
        }

        public ShakeSensor(ShakeListener sl)
        {
            mlistener = sl;
        }

        @Override
        public void onSensorChanged(SensorEvent event)
        {
            if (mlistener != null) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                float gX = x / SensorManager.GRAVITY_EARTH;
                float gY = y / SensorManager.GRAVITY_EARTH;
                float gZ = z / SensorManager.GRAVITY_EARTH;

                // gForce will be close to 1 when there is no movement.
                float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

                if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                    final long now = System.currentTimeMillis();
                    // ignore shake events too close to each other (500ms)
                    if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                        return;
                    }

                    // reset the shake count after 3 seconds of no shakes
                    if (mShakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                        mShakeCount = 0;
                    }

                    mShakeTimestamp = now;
                    mShakeCount++;

                    mlistener.onShake();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i)
        {
            // do nothing
        }
    }

    private static class WLD implements Serializable
    {
        public int wins;
        public int losses;
        public int draws;

        public static final long serializableVersionUID = 2L;

        public WLD(int w, int l, int d)
        {
            wins = w;
            losses = l;
            draws = d;
        }

        public WLD()
        {
            wins = -1;
            losses = -1;
            draws = -1;
        }

        @Override
        public String toString()
        {
            return "(" + wins + "/" + losses + "/" + draws + ")";
        }
    }

    private static class GameView extends View implements ShakeSensor.ShakeListener {
        private ArrayList<Pair<Point>> gridlines;
        private static final ArrayList<Pair<Integer>> winpairs;
        private Context parent;

        private int spaces[];
        private boolean keepGoing;

        private boolean xFirstMove;

        private MediaPlayer sound;

        public GameView self;

        private Drawable XDraw;
        private Drawable ODraw;

        private static final int OPEN = 0, HAS_X = 1, HAS_O = 2;

        static {
            winpairs = new ArrayList<Pair<Integer>>();
            winpairs.add(new Pair<Integer>(0, 1));
            winpairs.add(new Pair<Integer>(3, 1));
            winpairs.add(new Pair<Integer>(6, 1));

            winpairs.add(new Pair<Integer>(0, 3));
            winpairs.add(new Pair<Integer>(1, 3));
            winpairs.add(new Pair<Integer>(2, 3));

            winpairs.add(new Pair<Integer>(0, 4));
            winpairs.add(new Pair<Integer>(2, 2));
        }

        public GameView(final Context context)
        {
            super(context);
            parent = context;

            XDraw = context.getResources().getDrawable(R.drawable.image_x);
            ODraw = context.getResources().getDrawable(R.drawable.image_o);

            setFocusable(true);

            gridlines = new ArrayList<Pair<Point>>();

            spaces = new int[9];
            for (int x = 0; x < 9; x++)
                spaces[x] = OPEN;

            keepGoing = true;

            xFirstMove = true;

            changeProperty("wins", 0);
            changeProperty("losses", 0);
            changeProperty("draws", 0);

            sound = MediaPlayer.create(context, R.raw.blop);

            self = this;

            setOnTouchListener(new OnTouchListener() {
                private GestureDetector gd = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e)
                    {
                        for (int x = 0; x < 9; x++)
                            self.spaces[x] = OPEN;

                        self.invalidate();

                        return true;
                    }

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent e)
                    {
                        self.touch(e);
                        return true;
                    }

                    @Override
                    public boolean onFling(MotionEvent m1, MotionEvent m2, float vx, float vy)
                    {
                        //displayInfo("boolean onFling(MotionEvent, MotionEvent, float, float");

                        final AlertDialog.Builder dialog = new AlertDialog.Builder(parent);
                        dialog.setMessage("Erase Current Scores?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try {
                                    File file = getScoresFile();
                                    FileOutputStream fos = new FileOutputStream(file);
                                    ObjectOutputStream oos = new ObjectOutputStream(fos);

                                    oos.writeObject(new WLD(0, 0, 0));
                                }
                                catch(Exception ex) {
                                    Log.e("onFling dialog", "Error erasing scores: " + ex.getMessage());
                                    System.exit(0);
                                }
                            }
                        }).setNegativeButton("Nah", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss(); // not sure which does what
                                dialogInterface.cancel();
                            }
                        });
                        dialog.show();

                        return true;
                    }
                });

                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    gd.onTouchEvent(motionEvent);
                    return true;
                }
            });
        }

        private int getIndexFor(Point p)
        {
            if (p.y < getHeight() / 3) {
                if (p.x < getWidth() / 3)
                    return 0;
                else if (p.x < 2 * getWidth() / 3)
                    return 1;
                else
                    return 2;
            } else if (p.y < 2 * getHeight() / 3) {
                if (p.x < getWidth() / 3)
                    return 3;
                else if (p.x < 2 * getWidth() / 3)
                    return 4;
                else
                    return 5;
            } else {
                if (p.x < getWidth() / 3)
                    return 6;
                else if (p.x < 2 * getWidth() / 3)
                    return 7;
                else
                    return 8;
            }
        }

        /*@Override
        public void onShake()
        {
            String bundle = "com.example.eppsna01.tictactoe2";

            SharedPreferences prefs = parent.getSharedPreferences(bundle, Context.MODE_PRIVATE);

            prefs.edit().putLong(bundle + ".wins", 0).commit();
            prefs.edit().putLong(bundle + ".losses", 0).commit();
            prefs.edit().putLong(bundle + ".draws", 0).commit();
        }*/

        @Override
        public void onShake()
        {
            AlertDialog.Builder dialog = new AlertDialog.Builder(parent);
            dialog.setMessage("Erase scores?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try {
                        File file = getScoresFile();
                        FileOutputStream fos = new FileOutputStream(file, false);
                        ObjectOutputStream oos = new ObjectOutputStream(fos);

                        oos.writeObject(new WLD(0, 0, 0));
                    }
                    catch(Exception ex) {
                        Log.e("onShake()", "Error creating file object");
                        System.exit(0);
                    }
                }
            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
        }

        /*private void changePropertyPrefs(String name, long inc) {
            String bundle = "com.example.eppsna01.tictactoe2";
            SharedPreferences prefs = parent.getSharedPreferences(bundle, Context.MODE_PRIVATE);

            long data = prefs.getLong(bundle + "." + name, 0);
            data += inc;

            if (!prefs.edit().putLong(bundle + "." + name, data).commit()) {
                Log.e("changeProperty()", "Error changing property");
            }
        }*/

        private void writeZeros(File f) throws IOException
        {
            String fname = "writeZeros(File)";

            try {
                if (f.createNewFile()) {
                    Log.e(fname, "Writing zeros...");
                    FileOutputStream fos = new FileOutputStream(f);
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    oos.writeObject(new WLD(0, 0, 0));
                }
            }
            catch (IOException ioe) {
                Log.e(fname, "IOException: " + ioe.getMessage());
                Log.e(fname, "Stack Trace: ");
                StackTraceElement[] stackTrace = ioe.getStackTrace();

                for (int x = 0; x < stackTrace.length; x++)
                    Log.e(fname, stackTrace[x].toString());

                System.exit(0);
            }
        }

        private File getScoresFile() throws IOException
        {
            Log.d("getScoresFile()", "Getting the scores file object...");

            String[] files = parent.fileList();
            boolean found = false;
            File scores = null;

            for (String f : files) {
                if (f.equals("scores")) {
                    found = true;
                    scores = new File(parent.getFilesDir(), "scores");
                    break;
                }
            }

            writeZeros(scores);

            if (!found) {
                scores = new File(parent.getFilesDir(), "scores");
                if(scores.createNewFile())
                    Log.e("getScoresFile", "File successfully created");
                else
                    Log.e("getScoresFile", "not found, but file already exists?");

                // do this if file doesn't exist???
                writeZeros(scores);

                return scores;
            }

            return scores;
        }

        private void changePropertyFile(String name, long inc)
        {
            String fname = "changePropertyFile";
            Log.d(fname, "calling changePropertyFile(String, long)...");

            File file = null;
            FileInputStream fis = null;
            ObjectInputStream ois = null;
            WLD wld = null;

            try {
                file = getScoresFile();
                if (file == null)
                    Log.e(fname, "file == null");
                if (file.createNewFile())
                    Log.e(fname, "created file?? (not good)");
                else
                    Log.d(fname, "File didn't need to be created (good)");

                fis = new FileInputStream(file);
                if (fis == null)
                    Log.e(fname, "fis == null");

                ois = new ObjectInputStream(fis);
                if (ois == null)
                    Log.e(fname, "ois == null");

                wld = (WLD) ois.readObject();
                if (wld == null)
                    Log.e(fname, "wld == null");
            }
            catch (StreamCorruptedException sce) {
                Log.e(fname, "Stream is corrupted");
                System.exit(0);
            }
            catch (IOException ioe) {
                Log.e(fname, "I/O error occurred");
                System.exit(0);
            }
            catch (SecurityException se) {
                Log.e(fname, "Security exception??");
                System.exit(0);
            }
            catch(Exception ex) {
                Log.e(fname, "Other exception: " + ex.getMessage());
                System.exit(0);
            }

            if (name == "wins")
                wld.wins += inc;
            else if (name == "losses")
                wld.losses += inc;
            else if (name == "draws")
                wld.draws += inc;
            else {
                Log.e(fname, "Invalid \"name\": " + name);
                System.exit(0);
            }

            try {
                FileOutputStream fos = new FileOutputStream(file);
                ObjectOutputStream oos = new ObjectOutputStream(fos);

                oos.writeObject(wld);
            }
            catch(Exception ex) {
                Log.e(fname, "Error during output");
                System.exit(0);
            }

            /*File file = null;

            try {
                file = getScoresFile();
            } catch (IOException e) {
                StackTraceElement elements[] = e.getStackTrace();
                String fname = "changePropertyFile";

                Log.e(fname, "ERROR: Stack Trace: ");
                for (StackTraceElement ste : elements)
                    Log.e(fname, ste.toString());

                System.exit(0);
            }

            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
            } catch (Exception ex) {
                Log.e("changePropertyFile", "Could not create input stream");

                System.exit(0);
            }

            int data[] = new int[3];
            for (int x = 0; x < 3; x++) {
                try {
                    data[x] = fis.read();
                } catch (IOException e) {
                    StackTraceElement elements[] = e.getStackTrace();
                    String fname = "changePropertyFile";

                    Log.e(fname, "ERROR: Stack Trace: ");
                    for (StackTraceElement ste : elements)
                        Log.e(fname, ste.toString());

                    System.exit(0);
                }
            }

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file, false);
            } catch (Exception ex) {
                Log.e("changePropertyFile", "Could not create new FileOutputStream");
                System.exit(0);
            }

            if (name == "wins") {
                data[0] += inc;
            } else if (name == "losses") {
                data[1] += inc;
            } else if (name == "draws") {
                data[2] += inc;
            } else {
                Log.e("changePropertyFile", "Invalid \"name\" parameter");
                System.exit(0);
            }*/
        }

        private void changeProperty(String name, long inc)
        {
            changePropertyFile(name, inc);
        }

        /*private String getWLDStringPrefs()
        {
            String appname = "com.example.eppsna01.tictactoe2";

            SharedPreferences prefs = parent.getSharedPreferences(appname, Context.MODE_PRIVATE);

            return prefs.getLong(appname + ".wins", -1) + "/" + prefs.getLong(appname + ".losses", -1) + "/" + prefs.getLong(appname + ".draws", -1);
        }*/

        private String getWLDStringFile()
        {
            FileInputStream fis = null;
            ObjectInputStream ois = null;
            try {
                fis = new FileInputStream(getScoresFile());
                ois = new ObjectInputStream(fis);

                WLD wld = (WLD) ois.readObject();
                return wld.toString();
            }
            catch (Exception ex) {
                Log.e("getWLDStringFile", "Could not open/read from file");

                System.exit(0);
            }

            return "(error)";
        }

        private String getWLDString()
        {
            return getWLDStringFile();
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh)
        {
            int width = getWidth(), height = getHeight();

            gridlines.add(new Pair<Point>(new Point(width / 3, 0), new Point(width / 3, height)));
            gridlines.add(new Pair<Point>(new Point(2 * width / 3, 0), new Point(2 * width / 3, height)));

            gridlines.add(new Pair<Point>(new Point(0, height / 3), new Point(width, height / 3)));
            gridlines.add(new Pair<Point>(new Point(0, 2 * height / 3), new Point(width, 2 * height / 3)));
        }

        private void displayDialog(String mssg)
        {
            if (mssg.startsWith("You win!"))
                changeProperty("wins", 1);
            else if (mssg.startsWith("You lose!"))
                changeProperty("losses", 1);
            else if (mssg.startsWith("Draw!"))
                changeProperty("draws", 1);

            mssg += " W/L/D: " + getWLDString();

            AlertDialog.Builder builder = new AlertDialog.Builder(parent);
            builder.setMessage(mssg).setPositiveButton("Yeah, bruh", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    for (int x = 0; x < spaces.length; x++)
                        spaces[x] = OPEN;
                    if (!xFirstMove)
                        playO();
                    invalidate();
                }
            }).setNegativeButton("Nah, fam", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    System.exit(0);
                }
            });
            builder.show();
        }

        private static boolean isWin(final int index, final int inc, int[] spaces)
        {
            return spaces[index] == spaces[index + inc] && spaces[index + inc] == spaces[index + inc * 2] && spaces[index] != OPEN;
        }

        private static boolean checkWin(int[] arr, GameView obj)
        {
            for (Pair<Integer> pi : winpairs)
            {
                if (isWin(pi.first, pi.second, arr))
                {
                    String mssg;

                    if (arr[pi.first] == HAS_X) {
                        mssg = "You win!";
                    }
                    else {
                        mssg = "You lose!";
                    }

                    mssg += " Play again?";

                    if (obj != null) {
                        obj.displayDialog(mssg);
                        obj.xFirstMove = !obj.xFirstMove;
                    }

                    return true;
                }
            }

            return false;
        }

        private int firstPossibleOWin()
        {
            int[] spacesCopy = new int[spaces.length];
            System.arraycopy(spaces, 0, spacesCopy, 0, spaces.length);

            for (int x = 0; x < 9; x++)
            {
                if (spacesCopy[x] != OPEN)
                    continue;

                spacesCopy[x] = HAS_O;
                if (checkWin(spacesCopy, null))
                    return x;

                spacesCopy[x] = OPEN;
            }

            return -1;
        }

        private int spotToBlockX()
        {
            int[] spacesCopy = new int[spaces.length];
            System.arraycopy(spaces, 0, spacesCopy, 0, spaces.length);

            for (int x = 0; x < 9; x++)
            {
                if (spaces[x] != OPEN)
                    continue;

                spacesCopy[x] = HAS_X;
                if (checkWin(spacesCopy, null))
                    return x;

                spacesCopy[x] = OPEN;
            }

            return -1;
        }

        private void playO()
        {
            ArrayList<Integer> openIndexes = new ArrayList<Integer>();

            for (int x = 0; x < 9; x++)
                if (spaces[x] == OPEN) openIndexes.add(x);

            if (openIndexes.size() == 0) {
                displayDialog("Draw! Play again?");
                return;
            }

            int aiMove = firstPossibleOWin(), aiBlock = spotToBlockX(), aiRand = openIndexes.get(randInt(0, openIndexes.size() - 1));

            int index = -1;

            if (aiMove != -1)
                index = aiMove;
            else if (aiBlock != -1)
                index = aiBlock;
            else
                index = aiRand;

            spaces[index] = HAS_O;

            // catch possible draw
            openIndexes.clear();
            for (int x = 0; x < 9; x++)
                if (spaces[x] == OPEN) openIndexes.add(x);

            if (openIndexes.size() == 0)
                displayDialog("Draw! Play again?");
        }

        public void touch(MotionEvent me)
        {
            if (!keepGoing || me.getAction() != MotionEvent.ACTION_DOWN)
                return;// true;

            sound.start(); // play the sound effect

            Point point = new Point((int) me.getX(), (int) me.getY());

            int index = getIndexFor(point);

            if (spaces[index] == OPEN)
            {
                spaces[index] = HAS_X;
                invalidate();
                if (checkWin(spaces, this)) {
                    return;// true;
                }
                invalidate();
                playO();
                invalidate();
                checkWin(spaces, this);
                invalidate();
            }

            //return true;
        }

        @Override
        protected void onDraw(Canvas canvas)
        {
            canvas.drawColor(Color.WHITE);

            Paint paint = new Paint();
            int w = canvas.getWidth();

            paint.setAntiAlias(true);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth((float) (w * 0.0075));

            for (Pair<Point> gp : gridlines)
                canvas.drawLine(gp.first.x, gp.first.y, gp.second.x, gp.second.y, paint);

            int sqWidth = getWidth() / 3;
            int sqHeight = getHeight() / 3;

            int xpos = 0, ypos = 0;

            for (int i : spaces)
            {
                if (i != OPEN)
                {
                    Rect r = new Rect();
                    r.left = xpos;
                    r.right = xpos + sqWidth;
                    r.top = ypos;
                    r.bottom = ypos + sqHeight;

                    Drawable draw = null;

                    if (i == HAS_X) {
                        draw = XDraw;
                    } else if (i == HAS_O) {
                        draw = ODraw;
                    }

                    draw.setBounds(r);
                    draw.draw(canvas);
                }

                /*if (i == HAS_X)
                {
                    paint.setColor(Color.BLUE);
                    canvas.drawLine(xpos, ypos, xpos + sqWidth, ypos + sqHeight, paint);
                    canvas.drawLine(xpos, ypos + sqHeight, xpos + sqWidth, ypos, paint);
                }

                else if (i == HAS_O)
                {
                    paint.setColor(Color.RED);
                    canvas.drawCircle(xpos + sqWidth / 2, ypos + sqHeight / 2, Math.min(sqWidth, sqHeight) / 2, paint);
                }*/

                xpos += sqWidth;
                if (xpos >= 3 * getWidth() / 4) {
                    xpos = 0;
                    ypos += sqHeight;
                }
            }
        }
    }
}