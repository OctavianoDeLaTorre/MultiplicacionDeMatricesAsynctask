package com.octaviano.multmatrices;

import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private TextView lblDimension;
    private TextView lblM1;
    private TextView lblM2;
    private TextView lblRes;
    private TextView txtTiempo;
    private SeekBar sbDimencion;
    private TextView lblNumThreads;
    private SeekBar sbNumThreads;
    private int m1[][];
    private int m2[][];
    private int mRes[][];
    protected volatile int tActivos;
    private int dimension;
    private int numThreads;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * Instaciar vistas
         */
        lblDimension = findViewById(R.id.lblDimension);
        sbDimencion = findViewById(R.id.sbDimension);
        lblNumThreads = findViewById(R.id.lblNumThreads);
        sbNumThreads = findViewById(R.id.sbNumThreads);
        txtTiempo = findViewById(R.id.txtTiempo);
        /**
         * Instanciar matrices
         */
        lblM1 = findViewById(R.id.txtM1);
        lblM2 = findViewById(R.id.lblM2);
        lblRes = findViewById(R.id.lblRes);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            sbNumThreads.setMin(1);
            sbDimencion.setMin(1);
        }


        sbDimencion.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (progress%2 != 0 && progress != 1){
                        sbDimencion.setProgress(progress+1);
                    }
                    sbNumThreads.setMax(sbDimencion.getProgress());
                    lblDimension.setText(sbDimencion.getProgress()+"x"+sbDimencion.getProgress());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        sbNumThreads.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (progress%2 != 0){
                        sbNumThreads.setProgress(progress+1);
                    }
                    lblNumThreads.setText(String.valueOf(sbNumThreads.getProgress()));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    /**
     * Metodo para capturar el evento del boton
     * @param v
     */
    public void multiplicar(View v){
        try {
            dimension = sbDimencion.getProgress();
            numThreads = sbNumThreads.getProgress();
            m1 = new GenerarMatriz().execute(dimension,dimension).get();
            m2 = new GenerarMatriz().execute(dimension, dimension).get();
            mostrarMatriz(m1,lblM1);
            mostrarMatriz(m2,lblM2);
            mRes = new int[dimension][dimension];
            ArrayList<Vector<Integer>> particiones = dividirTarea(dimension,numThreads);
            tActivos  = numThreads;

            for (int i = 0; i < numThreads; i++){
                new MultMatriz(particiones.get(i)).execute();
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Metodo para dividir las columnas de la matriz que se
     * multiplicaran por cada thread.
     *
     * @param tamañoMatriz #Dimension de la matriz
     * @param numThreads #Numero de thread que realizaran la
     *                   multiplicacion
     * @return  #ArrayList que contiene los vectores con las
     *          columnas que multiplicara cada thread.
     */
    public ArrayList<Vector<Integer>> dividirTarea(int tamañoMatriz, int numThreads){
        int aux = 0;
        ArrayList<Vector<Integer>> particiones = new ArrayList<>();
        for (int i = 0; i < numThreads; i++){
            particiones.add(new Vector<Integer>());
        }
        for(int i = 0; i < tamañoMatriz; i++){
            if(aux == numThreads){
                aux = 0;
            }
            particiones.get(aux).add(i);
            aux++;
        }
        return  particiones;
    }


    /**
     * Clase para generar matriz de numero aleatorios
     */
    private class GenerarMatriz extends AsyncTask <Integer, Void, int [][]>{
        @Override
        protected int [][] doInBackground(Integer... args) {
            Random nAleatorio = new Random(225);
            int x = args[0];
            int y = args[1];
            int matriz[][] = new int[x][y];
            for (int i = 0; i < x; i++) {
                for (int j = 0; j < y; j++) {
                    matriz[i][j] = nAleatorio.nextInt(9 - 1 + 1) + 1;
                }
            }
            return matriz;
        }
    }


    /**
     * Clase para multiplicar la matriz
     */
    private class MultMatriz extends AsyncTask <Void , Void, Void> {
        /**
         *Vector que contiene el las columnas que se
         *ultiplicaran
         */
        Vector<Integer> columnas;
        /**
         * Variables para almacenar el tiepo de ejecucion
         * */
        long tiempo;

        /**
         * Cosntructor de la clase
         *
         * @param columnas #Vector que contiene las
         *                 columnas a multiplicar
         */
        MultMatriz(Vector<Integer> columnas){
            this.columnas = columnas;
        }

        @Override
        /**
         * Realiza la multiplicacion de las columnas contenidad
         * en el vector.
         */
        protected Void doInBackground(Void... integers) {
            long inicio = System.currentTimeMillis();
            for (int i = 0; i < columnas.size(); i++) {
                for (int j = 0; j < m2[0].length ; j++) {
                    int suma = 0;
                    for (int k = 0; k < m1[0].length; k++) {
                        suma += (m1[columnas.get(i)][k] * m2[k][j]);
                    }
                    mRes[columnas.get(i)][j] = suma;
                }
            }
            tiempo = System.currentTimeMillis()-inicio;
           tActivos--;
            return null;
        }

        @Override
        /**
         * Muestra la matriz y el tiempo de ejecucion
         * en caso de que sea el ultimo thread.
         */
        protected void onPostExecute(Void aVoid) {
            if (tActivos == 0){
                mostrarMatriz(mRes,lblRes);
                txtTiempo.setText("Tiempo de ejecucion: "+tiempo+" ms");
                Toast.makeText(MainActivity.this,"Tiempo de ejecucion: "+tiempo+" ms",Toast.LENGTH_LONG).show();
            }

        }
    }

    /**
     * Muestra la matriz en una etiqueta.
     *
     * @param matriz #Matriz que se mostrara
     * @param lbl   #Etiqueta donde se mostrara lka matriz
     */
    public void mostrarMatriz(int matriz[][], TextView lbl) {
        lbl.setText("");
        for (int i = 0; i < matriz.length; i++) {
            lbl.setText(lbl.getText()+"\n");
            for (int j = 0; j < matriz.length; j++) {
                lbl.setText(lbl.getText()+" , "+matriz[i][j]);
            }
        }
    }
}
