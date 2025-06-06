import { GoogleGenAI } from "@google/genai";
import express from 'express';
import multer from "multer";
import fs from 'fs/promises';
import cors from 'cors';
import pdf from 'pdf-parse';
import dotenv from 'dotenv';
dotenv.config();

const app = express();
const PORT = 3000;

const upload = multer({dest:'uploads/'});

const APIKEY = process.env.APIKEY
const ai = new GoogleGenAI({apiKey:APIKEY});

async function Ai(prompt: string): Promise<string> {
    const response = await ai.models.generateContent({
        model: "gemini-2.0-flash",
        contents: prompt
    });
    return response.text || "No feedback received from AI.";
}

app.use(cors({
  origin: 'http://localhost:3001'
}));

app.post('/upload', upload.single('resume'), async (req, res): Promise<void> => {
  console.log("Called");
  try {
    const filePath = req.file?.path;
    if (!filePath) {
      res.status(400).json({ error: 'File not uploaded' });
      return;
    }

    const buffer = await fs.readFile(filePath);
    const pdfData = await pdf(buffer);
    const feedback = await Ai(`You are a professional resume builder and analyzer. Analyze this resume data and provide:
      1. Things that need improvement.
      2. Things that are good.
      3. An overall review score out of 5.
      Resume data: \n${pdfData.text}`);

    await fs.unlink(filePath);
    res.json({ feedback });
  } catch (error) {
    console.error('Error:', error);
    res.status(500).json({ error: error });
  }
});


app.listen(PORT, () => {
  console.log(`Server is running on http://localhost:${PORT}`);
});