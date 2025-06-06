"use strict";
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const genai_1 = require("@google/genai");
const express_1 = __importDefault(require("express"));
const multer_1 = __importDefault(require("multer"));
const promises_1 = __importDefault(require("fs/promises"));
const cors_1 = __importDefault(require("cors"));
const pdf_parse_1 = __importDefault(require("pdf-parse"));
const dotenv_1 = __importDefault(require("dotenv"));
dotenv_1.default.config();
const app = (0, express_1.default)();
const PORT = 3000;
const upload = (0, multer_1.default)({ dest: 'uploads/' });
const APIKEY = process.env.APIKEY;
const ai = new genai_1.GoogleGenAI({ apiKey: APIKEY });
function Ai(prompt) {
    return __awaiter(this, void 0, void 0, function* () {
        const response = yield ai.models.generateContent({
            model: "gemini-2.0-flash",
            contents: prompt
        });
        return response.text || "No feedback received from AI.";
    });
}
app.use((0, cors_1.default)({
    origin: 'http://localhost:3001'
}));
app.post('/upload', upload.single('resume'), (req, res) => __awaiter(void 0, void 0, void 0, function* () {
    var _a;
    console.log("Called");
    try {
        const filePath = (_a = req.file) === null || _a === void 0 ? void 0 : _a.path;
        if (!filePath) {
            res.status(400).json({ error: 'File not uploaded' });
            return;
        }
        const buffer = yield promises_1.default.readFile(filePath);
        const pdfData = yield (0, pdf_parse_1.default)(buffer);
        const feedback = yield Ai(`You are a professional resume builder and analyzer. Analyze this resume data and provide:
      1. Things that need improvement.
      2. Things that are good.
      3. An overall review score out of 5.
      Resume data: \n${pdfData.text}`);
        yield promises_1.default.unlink(filePath);
        res.json({ feedback });
    }
    catch (error) {
        console.error('Error:', error);
        res.status(500).json({ error: error });
    }
}));
app.listen(PORT, () => {
    console.log(`Server is running on http://localhost:${PORT}`);
});
