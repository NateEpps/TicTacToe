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
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends Activity
{
    private MediaPlayer music;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(new MainMenu(this));

        music = MediaPlayer.create(this, R.raw.music);
        music.start();
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

    private static class GameView extends View
    {
        private ArrayList<Pair<Point>> gridlines;
        private static final ArrayList<Pair<Integer>> winpairs;
        private Context parent;

        private int spaces[];
        private boolean keepGoing;

        private boolean xFirstMove;

        private MediaPlayer sound;

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

        public GameView(Context context) {
            super(context);
            setFocusable(true);

            parent = context;

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
        }

        private int getIndexFor(Point p) {
            if (p.y < getHeight() / 3) {
                if (p.x < getWidth() / 3)
                    return 0;
                else if (p.x < 2 * getWidth() / 3)
                    return 1;
                else
                    return 2;
            }
            else if (p.y < 2 * getHeight() / 3) {
                if (p.x < getWidth() / 3)
                    return 3;
                else if (p.x < 2 * getWidth() / 3)
                    return 4;
                else
                    return 5;
            }
            else {
                if (p.x < getWidth() / 3)
                    return 6;
                else if (p.x < 2 * getWidth() / 3)
                    return 7;
                else
                    return 8;
            }
        }

        private void changeProperty(String name, long inc)
        {
            SharedPreferences prefs = parent.getSharedPreferences("com.example.eppsna01.tictactoe2", Context.MODE_PRIVATE);

            long data = prefs.getLong("com.example.eppsna01.tictactoe2." + name, 0);
            data += inc;

            if (!prefs.edit().putLong("com.example.eppsna01.tictactoe2." + name, data).commit()) {
                Log.e("changeProperty()", "Error changing property");
            }
        }

        private String getWLDString()
        {
            String appname = "com.example.eppsna01.tictactoe2";

            SharedPreferences prefs = parent.getSharedPreferences(appname, Context.MODE_PRIVATE);

            return prefs.getLong(appname + ".wins", -1) + "/" + prefs.getLong(appname + ".losses", -1) + "/" + prefs.getLong(appname + ".draws", -1);
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

        private void playO()
        {
            ArrayList<Integer> openIndexes = new ArrayList<Integer>();

            for (int x = 0; x < 9; x++)
                if (spaces[x] == OPEN) openIndexes.add(x);

            if (openIndexes.size() == 0) {
                displayDialog("Draw! Play again?");
                return;
            }

            int aiMove = firstPossibleOWin(), index = 0;
            if (aiMove == -1) {
                index = openIndexes.get(randInt(0, openIndexes.size() - 1));
                if (index < 0 || index >= spaces.length)
                    throw new IndexOutOfBoundsException();
            }
            else {
                index = aiMove;
            }

            spaces[index] = HAS_O;

            // catch possible draw
            openIndexes.clear();
            for (int x = 0; x < 9; x++)
                if (spaces[x] == OPEN) openIndexes.add(x);

            if (openIndexes.size() == 0)
                displayDialog("Draw! Play again?");
        }

        @Override
        public boolean onTouchEvent(MotionEvent me)
        {
            if (!keepGoing || me.getAction() != MotionEvent.ACTION_DOWN)
                return true;

            sound.start(); // play the sound effect

            Point point = new Point((int) me.getX(), (int) me.getY());

            int index = getIndexFor(point);

            if (spaces[index] == OPEN)
            {
                spaces[index] = HAS_X;
                invalidate();
                if (checkWin(spaces, this)) {
                    return true;
                }
                invalidate();
                playO();
                invalidate();
                checkWin(spaces, this);
                invalidate();
            }

            return true;
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
                if (i == HAS_X)
                {
                    paint.setColor(Color.BLUE);
                    canvas.drawLine(xpos, ypos, xpos + sqWidth, ypos + sqHeight, paint);
                    canvas.drawLine(xpos, ypos + sqHeight, xpos + sqWidth, ypos, paint);
                }

                else if (i == HAS_O)
                {
                    paint.setColor(Color.RED);
                    canvas.drawCircle(xpos + sqWidth / 2, ypos + sqHeight / 2, Math.min(sqWidth, sqHeight) / 2, paint);
                }

                xpos += sqWidth;
                if (xpos >= 3 * getWidth() / 4) {
                    xpos = 0;
                    ypos += sqHeight;
                }
            }
        }
    }
}