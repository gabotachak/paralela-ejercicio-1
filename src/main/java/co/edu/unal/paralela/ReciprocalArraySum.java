package co.edu.unal.paralela;

import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ForkJoinPool;

public final class ReciprocalArraySum {

    private ReciprocalArraySum() {
    }

    /**
     * Calcula secuencialmente la suma de valores recíprocos para un arreglo.
     *
     * @param input Arreglo de entrada
     * @return La suma de los recíprocos del arreglo de entrada
     */
    protected static double seqArraySum(final double[] input) {
        double sum = 0;
        for (int i = 0; i < input.length; i++) {
            sum += 1 / input[i];
        }
        return sum;
    }

    private static int getChunkSize(final int nChunks, final int nElements) {
        return (nElements + nChunks - 1) / nChunks;
    }

    private static int getChunkStartInclusive(final int chunk, final int nChunks, final int nElements) {
        final int chunkSize = getChunkSize(nChunks, nElements);
        return chunk * chunkSize;
    }

    private static int getChunkEndExclusive(final int chunk, final int nChunks, final int nElements) {
        final int chunkSize = getChunkSize(nChunks, nElements);
        final int end = (chunk + 1) * chunkSize;
        return end > nElements ? nElements : end;
    }

    /**
     * Este pedazo de clase puede ser completada para para implementar el cuerpo de
     * cada tarea creada
     * para realizar la suma de los recíprocos del arreglo en paralelo.
     */
    private static class ReciprocalArraySumTask extends RecursiveAction {
        private final int startIndexInclusive;
        private final int endIndexExclusive;
        private final double[] input;
        private double value;

        ReciprocalArraySumTask(final int setStartIndexInclusive, final int setEndIndexExclusive,
                final double[] setInput) {
            this.startIndexInclusive = setStartIndexInclusive;
            this.endIndexExclusive = setEndIndexExclusive;
            this.input = setInput;
        }

        /**
         * Adquiere el valor calculado por esta tarea.
         *
         * @return El valor calculado por esta tarea
         */
        public double getValue() {
            return value;
        }

        @Override
        protected void compute() {
            if (endIndexExclusive - startIndexInclusive <= 10000) { // Threshold for sequential computation
                for (int i = startIndexInclusive; i < endIndexExclusive; i++) {
                    value += 1 / input[i];
                }
            } else {
                int mid = (startIndexInclusive + endIndexExclusive) / 2;
                ReciprocalArraySumTask left = new ReciprocalArraySumTask(startIndexInclusive, mid, input);
                ReciprocalArraySumTask right = new ReciprocalArraySumTask(mid, endIndexExclusive, input);

                left.fork();
                right.compute();
                left.join();

                value = left.getValue() + right.getValue();
            }
        }
    }

    /**
     * Modificar para calcular la suma en paralelo utilizando dos tareas.
     *
     * @param input Arreglo de entrada
     * @return La suma de los recíprocos del arreglo de entrada
     */
    protected static double parArraySum(final double[] input) {
        assert input.length % 2 == 0;

        // Crear y ejecutar las tareas en paralelo
        ReciprocalArraySumTask leftTask = new ReciprocalArraySumTask(0, input.length / 2, input);
        ReciprocalArraySumTask rightTask = new ReciprocalArraySumTask(input.length / 2, input.length, input);

        ForkJoinPool.commonPool().invoke(leftTask);
        rightTask.compute();
        leftTask.join();

        return leftTask.getValue() + rightTask.getValue();
    }

    /**
     * Calcula la suma de los recíprocos del arreglo de entrada utilizando un número
     * establecido de tareas.
     *
     * @param input    Arreglo de entrada
     * @param numTasks El número de tareas para crear
     * @return La suma de los recíprocos del arreglo de entrada
     */
    protected static double parManyTaskArraySum(final double[] input, final int numTasks) {
        // Umbral definido en el método compute()
        final int THRESHOLD = 10000;
        int nElements = input.length;

        // Calcular el número de tareas necesarias basadas en el umbral
        int numTasksNeeded = (nElements + THRESHOLD - 1) / THRESHOLD;

        // Crear un arreglo para almacenar las tareas
        ReciprocalArraySumTask[] tasks = new ReciprocalArraySumTask[numTasksNeeded];

        // Crear las tareas dividiendo el trabajo en segmentos de tamaño <= THRESHOLD
        for (int i = 0; i < numTasksNeeded; i++) {
            int startIndex = i * THRESHOLD;
            int endIndex = Math.min((i + 1) * THRESHOLD, nElements);

            tasks[i] = new ReciprocalArraySumTask(startIndex, endIndex, input);
        }

        // Crear un ForkJoinPool con el nivel de paralelismo deseado
        ForkJoinPool pool = new ForkJoinPool(numTasks);

        // Ejecutar todas las tareas en el pool
        for (ReciprocalArraySumTask task : tasks) {
            pool.execute(task);
        }

        // Esperar a que todas las tareas terminen y acumular los resultados
        double sum = 0;
        for (ReciprocalArraySumTask task : tasks) {
            task.join();
            sum += task.getValue();
        }

        // Cerrar el pool
        pool.shutdown();

        return sum;
    }

}
