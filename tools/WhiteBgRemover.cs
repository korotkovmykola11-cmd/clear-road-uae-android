using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;

public static class WhiteBgRemover
{
    private static byte[] sBytes;
    private static int sTolerance;
    private static int sWidth;
    private static int sHeight;

    private static bool IsNearWhite(int i)
    {
        int b = sBytes[i];
        int g = sBytes[i + 1];
        int r = sBytes[i + 2];
        int a = sBytes[i + 3];
        if (a < 10) return true;
        return Math.Abs(r - 255) <= sTolerance
            && Math.Abs(g - 255) <= sTolerance
            && Math.Abs(b - 255) <= sTolerance;
    }

    private static void SetTransparent(int i)
    {
        sBytes[i + 3] = 0;
    }

    private static void Enqueue(int x, int y, bool[] visited, Queue<int> queue)
    {
        if (x < 0 || y < 0 || x >= sWidth || y >= sHeight) return;
        int idx = y * sWidth + x;
        if (visited[idx]) return;
        int bi = idx * 4;
        if (!IsNearWhite(bi)) return;
        visited[idx] = true;
        queue.Enqueue(idx);
    }

    public static void Remove(string inputPath, string outputPath, int tolerance)
    {
        sTolerance = tolerance;

        using (var src = new Bitmap(inputPath))
        using (var bmp = new Bitmap(src.Width, src.Height, PixelFormat.Format32bppArgb))
        {
            using (var g = Graphics.FromImage(bmp))
            {
                g.DrawImage(src, 0, 0, src.Width, src.Height);
            }

            sWidth = bmp.Width;
            sHeight = bmp.Height;
            var rect = new Rectangle(0, 0, sWidth, sHeight);
            var data = bmp.LockBits(rect, ImageLockMode.ReadWrite, PixelFormat.Format32bppArgb);
            sBytes = new byte[Math.Abs(data.Stride) * sHeight];
            Marshal.Copy(data.Scan0, sBytes, 0, sBytes.Length);

            var visited = new bool[sWidth * sHeight];
            var queue = new Queue<int>();

            for (int x = 0; x < sWidth; x++)
            {
                Enqueue(x, 0, visited, queue);
                Enqueue(x, sHeight - 1, visited, queue);
            }

            for (int y = 0; y < sHeight; y++)
            {
                Enqueue(0, y, visited, queue);
                Enqueue(sWidth - 1, y, visited, queue);
            }

            while (queue.Count > 0)
            {
                int idx = queue.Dequeue();
                int x = idx % sWidth;
                int y = idx / sWidth;
                SetTransparent(idx * 4);
                Enqueue(x - 1, y, visited, queue);
                Enqueue(x + 1, y, visited, queue);
                Enqueue(x, y - 1, visited, queue);
                Enqueue(x, y + 1, visited, queue);
            }

            Marshal.Copy(sBytes, 0, data.Scan0, sBytes.Length);
            bmp.UnlockBits(data);
            bmp.Save(outputPath, ImageFormat.Png);
        }
    }

    public static void Main(string[] args)
    {
        if (args.Length < 3)
        {
            Console.Error.WriteLine("Usage: WhiteBgRemover <input> <output> <tolerance>");
            Environment.Exit(1);
        }

        Remove(args[0], args[1], int.Parse(args[2]));
    }
}
