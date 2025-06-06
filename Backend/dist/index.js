import "dotenv/config";
import { GoogleGenerativeAI } from "@google/generative-ai";
import express from "express";
import multer from "multer";
import fs from "fs/promises";
import pdf from "pdf-parse";
import PQueue from "p-queue";
const app = express();
const PORT = 3000;
const upload = multer({ dest: "uploads/" });
const APIKEY = process.env.APIKEY;
if (!APIKEY) {
    throw new Error("APIKEY environment variable is not set. Please set it in a .env file.");
}
const ai = new GoogleGenerativeAI(APIKEY);
const aiQueue = new PQueue({
    concurrency: 1,
    interval: 60000,
    intervalCap: 15,
});
async function aiAnalyze(prompt) {
    try {
        const model = ai.getGenerativeModel({ model: "gemini-1.5-flash" });
        const response = await model.generateContent(prompt);
        const text = response.response.text();
        return text || "No feedback received from AI.";
    }
    catch (error) {
        console.error("AI Error:", error);
        throw new Error("Failed to generate AI response");
    }
}
async function getAIResponse(prompt) {
    try {
        return (await aiQueue.add(() => aiAnalyze(prompt))) || "Unable to get data";
    }
    catch (error) {
        console.error("Queue Error:", error);
        throw new Error("Failed to process AI queue");
    }
}
app.post("/upload", upload.single("resume"), async (req, res) => {
    try {
        const filePath = req.file?.path;
        if (!filePath) {
            res.status(400).json({ error: "File not uploaded" });
            return;
        }
        const buffer = await fs.readFile(filePath);
        const pdfData = await pdf(buffer);
        const feedback = await getAIResponse(`You are a professional resume builder and analyzer. Analyze this resume data and provide:
      1. Things that need improvement.
      2. Things that are good.
      3. An overall review score out of 5.
      Resume data: \n${pdfData.text}`);
        await fs.unlink(filePath).catch((err) => console.error("Failed to delete file:", err));
        res.json({ feedback });
    }
    catch (error) {
        console.error("Error processing resume:", error);
        res.status(500).json({ error: "Failed to process resume" });
    }
});
app.listen(PORT, () => {
    console.log(`Server is running on http://localhost:${PORT}`);
});
