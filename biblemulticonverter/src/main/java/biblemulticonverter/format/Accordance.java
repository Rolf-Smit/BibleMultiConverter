package biblemulticonverter.format;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import biblemulticonverter.data.Bible;
import biblemulticonverter.data.Book;
import biblemulticonverter.data.BookID;
import biblemulticonverter.data.Chapter;
import biblemulticonverter.data.FormattedText;
import biblemulticonverter.data.FormattedText.ExtraAttributePriority;
import biblemulticonverter.data.FormattedText.FormattingInstructionKind;
import biblemulticonverter.data.FormattedText.LineBreakKind;
import biblemulticonverter.data.FormattedText.RawHTMLMode;
import biblemulticonverter.data.FormattedText.Visitor;
import biblemulticonverter.data.StandardVersification;
import biblemulticonverter.data.Verse;
import biblemulticonverter.data.Versification;
import biblemulticonverter.data.Versification.Reference;
import biblemulticonverter.data.VersificationSet;
import biblemulticonverter.data.VirtualVerse;

public class Accordance implements ExportFormat {

	public static final String[] HELP_TEXT = {
			"Export format for Accordance",
			"",
			"Usage: Accordance <outfile> [<element>=<formatting> [...]] [lineending|encoding|verseschema=<value>]",
			"",
			"Supported elements: H*, H1-H9, FN, VN, XREF, STRONG, MORPH, DICT, GRAMMAR, PL,",
			"                    B, I, U, L, F, S, P, D, T, W",
			"Every supported element is also supported with prefix PL: (when in prolog).",
			"The prefix CSS: can be used to style CSS rules.",
			"",
			"Supported formattings:",
			" -                         Do not include element content",
			" +                         Include content unformatted",
			" <format>[+<format>[+...]] Include content with given format",
			" <formatting>#<formatting> Include a (footnote) number with first formatting, ",
			"                           move element content to end of verse with second formatting",
			"",
			"Supported formats: PARENS, BRACKETS, BRACES, BR_START, PARA_START, BR_END, PARA_END,",
			"                   NOBREAK, BOLD, SMALL_CAPS, ITALIC, SUB, SUP, UNDERLINE",
			"as well as colors: BLACK, GRAY, WHITE, CHOCOLATE, BURGUNDY, RED, ORANGE, BROWN,",
			"                   YELLOW, CYAN, TURQUOISE, GREEN, OLIVE, FOREST, TEAL, SAPPHIRE,",
			"                   BLUE, NAVY, PURPLE, LAVENDER, MAGENTA",
			"",
			"Other supported options:",
			" lineending=cr|lf         Use CR or LF as line ending",
			" encoding=macroman,utf-8  Try first macroman, then UTF-8 as encoding",
			" verseschema=fillone      Fill missing verses starting from verse 1",
			" verseschema=fillzero     In some psalms, fill from verse 0",
			" verseschema=restrictkjv  Restrict allowed verses to KJV schema",
			" verseschema=<name>@<db>  Use verse schema from database to restrict and fill verses"
	};

	public static Map<BookID, String> BOOK_NAME_MAP = new EnumMap<>(BookID.class);

	static {
		BOOK_NAME_MAP.put(BookID.BOOK_Gen, "Gen.");
		BOOK_NAME_MAP.put(BookID.BOOK_Exod, "Ex.");
		BOOK_NAME_MAP.put(BookID.BOOK_Lev, "Lev.");
		BOOK_NAME_MAP.put(BookID.BOOK_Num, "Num.");
		BOOK_NAME_MAP.put(BookID.BOOK_Deut, "Deut.");
		BOOK_NAME_MAP.put(BookID.BOOK_Josh, "Josh.");
		BOOK_NAME_MAP.put(BookID.BOOK_Judg, "Judg.");
		BOOK_NAME_MAP.put(BookID.BOOK_Ruth, "Ruth");
		BOOK_NAME_MAP.put(BookID.BOOK_1Sam, "1Sam.");
		BOOK_NAME_MAP.put(BookID.BOOK_2Sam, "2Sam.");
		BOOK_NAME_MAP.put(BookID.BOOK_1Kgs, "1Kings");
		BOOK_NAME_MAP.put(BookID.BOOK_2Kgs, "2Kings");
		BOOK_NAME_MAP.put(BookID.BOOK_1Chr, "1Chr.");
		BOOK_NAME_MAP.put(BookID.BOOK_2Chr, "2Chr.");
		BOOK_NAME_MAP.put(BookID.BOOK_Ezra, "Ezra");
		BOOK_NAME_MAP.put(BookID.BOOK_Neh, "Neh.");
		BOOK_NAME_MAP.put(BookID.BOOK_Esth, "Esth.");
		BOOK_NAME_MAP.put(BookID.BOOK_Job, "Job");
		BOOK_NAME_MAP.put(BookID.BOOK_Ps, "Psa.");
		BOOK_NAME_MAP.put(BookID.BOOK_Prov, "Prov.");
		BOOK_NAME_MAP.put(BookID.BOOK_Eccl, "Eccl.");
		BOOK_NAME_MAP.put(BookID.BOOK_Song, "Song");
		BOOK_NAME_MAP.put(BookID.BOOK_Isa, "Is.");
		BOOK_NAME_MAP.put(BookID.BOOK_Jer, "Jer.");
		BOOK_NAME_MAP.put(BookID.BOOK_Lam, "Lam.");
		BOOK_NAME_MAP.put(BookID.BOOK_Ezek, "Ezek.");
		BOOK_NAME_MAP.put(BookID.BOOK_Dan, "Dan.");
		BOOK_NAME_MAP.put(BookID.BOOK_Hos, "Hos.");
		BOOK_NAME_MAP.put(BookID.BOOK_Joel, "Joel");
		BOOK_NAME_MAP.put(BookID.BOOK_Amos, "Amos");
		BOOK_NAME_MAP.put(BookID.BOOK_Obad, "Obad.");
		BOOK_NAME_MAP.put(BookID.BOOK_Jonah, "Jonah");
		BOOK_NAME_MAP.put(BookID.BOOK_Mic, "Mic.");
		BOOK_NAME_MAP.put(BookID.BOOK_Nah, "Nah.");
		BOOK_NAME_MAP.put(BookID.BOOK_Hab, "Hab.");
		BOOK_NAME_MAP.put(BookID.BOOK_Zeph, "Zeph.");
		BOOK_NAME_MAP.put(BookID.BOOK_Hag, "Hag.");
		BOOK_NAME_MAP.put(BookID.BOOK_Zech, "Zech.");
		BOOK_NAME_MAP.put(BookID.BOOK_Mal, "Mal.");
		BOOK_NAME_MAP.put(BookID.BOOK_Tob, "Tob.");
		BOOK_NAME_MAP.put(BookID.BOOK_Jdt, "Jud.");
		BOOK_NAME_MAP.put(BookID.BOOK_Wis, "Wis.");
		BOOK_NAME_MAP.put(BookID.BOOK_Sir, "Sir.");
		BOOK_NAME_MAP.put(BookID.BOOK_Bar, "Bar.");
		BOOK_NAME_MAP.put(BookID.BOOK_1Macc, "1Mac.");
		BOOK_NAME_MAP.put(BookID.BOOK_2Macc, "2Mac.");
		BOOK_NAME_MAP.put(BookID.BOOK_1Esd, "1Esdr.");
		BOOK_NAME_MAP.put(BookID.BOOK_PrMan, "Man.");
		BOOK_NAME_MAP.put(BookID.BOOK_3Macc, "3Mac.");
		BOOK_NAME_MAP.put(BookID.BOOK_2Esd, "2Esdr.");
		BOOK_NAME_MAP.put(BookID.BOOK_4Ezra, "4Ezra");
		BOOK_NAME_MAP.put(BookID.BOOK_3Macc, "3Mac.");
		BOOK_NAME_MAP.put(BookID.BOOK_4Macc, "4Mac.");
		BOOK_NAME_MAP.put(BookID.BOOK_Sus, "Sus.");
		BOOK_NAME_MAP.put(BookID.BOOK_Bel, "Bel");
		BOOK_NAME_MAP.put(BookID.BOOK_EpJer, "Let.");
		BOOK_NAME_MAP.put(BookID.BOOK_PssSol, "Sol.");
		BOOK_NAME_MAP.put(BookID.BOOK_1En, "Enoch");
		BOOK_NAME_MAP.put(BookID.BOOK_Odes, "Ode.");
		BOOK_NAME_MAP.put(BookID.BOOK_EpLao, "Laod.");
		BOOK_NAME_MAP.put(BookID.BOOK_Matt, "Matt.");
		BOOK_NAME_MAP.put(BookID.BOOK_Mark, "Mark");
		BOOK_NAME_MAP.put(BookID.BOOK_Luke, "Luke");
		BOOK_NAME_MAP.put(BookID.BOOK_John, "John");
		BOOK_NAME_MAP.put(BookID.BOOK_Acts, "Acts");
		BOOK_NAME_MAP.put(BookID.BOOK_Rom, "Rom.");
		BOOK_NAME_MAP.put(BookID.BOOK_1Cor, "1Cor.");
		BOOK_NAME_MAP.put(BookID.BOOK_2Cor, "2Cor.");
		BOOK_NAME_MAP.put(BookID.BOOK_Gal, "Gal.");
		BOOK_NAME_MAP.put(BookID.BOOK_Eph, "Eph.");
		BOOK_NAME_MAP.put(BookID.BOOK_Phil, "Phil.");
		BOOK_NAME_MAP.put(BookID.BOOK_Col, "Col.");
		BOOK_NAME_MAP.put(BookID.BOOK_1Thess, "1Th.");
		BOOK_NAME_MAP.put(BookID.BOOK_2Thess, "2Th.");
		BOOK_NAME_MAP.put(BookID.BOOK_1Tim, "1Tim.");
		BOOK_NAME_MAP.put(BookID.BOOK_2Tim, "2Tim.");
		BOOK_NAME_MAP.put(BookID.BOOK_Titus, "Titus");
		BOOK_NAME_MAP.put(BookID.BOOK_Phlm, "Philem.");
		BOOK_NAME_MAP.put(BookID.BOOK_Heb, "Heb.");
		BOOK_NAME_MAP.put(BookID.BOOK_Jas, "James");
		BOOK_NAME_MAP.put(BookID.BOOK_1Pet, "1Pet.");
		BOOK_NAME_MAP.put(BookID.BOOK_2Pet, "2Pet.");
		BOOK_NAME_MAP.put(BookID.BOOK_1John, "1John");
		BOOK_NAME_MAP.put(BookID.BOOK_2John, "2John");
		BOOK_NAME_MAP.put(BookID.BOOK_3John, "3John");
		BOOK_NAME_MAP.put(BookID.BOOK_Jude, "Jude");
		BOOK_NAME_MAP.put(BookID.BOOK_Rev, "Rev.");
	}

	private static Set<String> SUPPORTED_ELEMENTS = new HashSet<>(Arrays.asList(
			"B", "I", "U", "L", "F", "S", "P", "D", "T", "W", "VN", "XREF", "STRONG", "MORPH", "DICT", "GRAMMAR",
			"FN", "H1", "H2", "H3", "H4", "H5", "H6", "H7", "H8", "H9", "PL", "PL:XREF",
			"PL:B", "PL:I", "PL:U", "PL:L", "PL:F", "PL:S", "PL:P", "PL:D", "PL:T", "PL:W", "PL:FN",
			"PL:H1", "PL:H2", "PL:H3", "PL:H4", "PL:H5", "PL:H6", "PL:H7", "PL:H8", "PL:H9"));

	@Override
	public void doExport(Bible bible, String... exportArgs) throws Exception {
		boolean paraMarker = false;
		for (Book book : bible.getBooks()) {
			if (!BOOK_NAME_MAP.containsKey(book.getId())) {
				continue;
			}
			for (Chapter chapter : book.getChapters()) {
				for (Verse v : chapter.getVerses()) {
					if (v.getElementTypes(Integer.MAX_VALUE).contains("b")) {
						paraMarker = true;
						break;
					}
				}
				if (paraMarker)
					break;
			}
			if (paraMarker)
				break;
		}
		File mainFile = new File(exportArgs[0] + ".txt");
		File booknameFile = new File(exportArgs[0] + "-booknames.txt");
		Map<String, String[]> formatRules = new HashMap<>();
		Set<String> unformattedElements = new HashSet<>();
		parseFormatRule("VN=BOLD", formatRules);
		for (String rule : Arrays.asList("B=BOLD", "D=SMALL_CAPS", "I=ITALIC", "S=SUB", "P=SUP", "U=UNDERLINE", "W=RED")) {
			parseFormatRule(rule, formatRules);
			parseFormatRule("PL:" + rule, formatRules);
		}
		String lineEnding = "\n";
		String[] encodings = null;
		int verseSchema = -1;
		Versification versification = null;
		BitSet psalmSet = null;
		for (int i = 1; i < exportArgs.length; i++) {
			if (exportArgs[i].toLowerCase().equals("lineending=cr")) {
				lineEnding = "\r";
			} else if (exportArgs[i].toLowerCase().equals("lineending=lf")) {
				lineEnding = "\n";
			} else if (exportArgs[i].toLowerCase().equals("verseschema=fillone")) {
				verseSchema = 1;
			} else if (exportArgs[i].toLowerCase().equals("verseschema=fillzero")) {
				verseSchema = 0;
				psalmSet = new BitSet(151);
				psalmSet.set(3, 9 + 1);
				psalmSet.set(11, 32 + 1);
				psalmSet.set(34, 42 + 1);
				psalmSet.set(44, 70 + 1);
				psalmSet.set(72, 90 + 1);
				psalmSet.set(92);
				psalmSet.set(98);
				psalmSet.set(100, 103 + 1);
				psalmSet.set(108, 110 + 1);
				psalmSet.set(120, 134 + 1);
				psalmSet.set(138, 145 + 1);
			} else if (exportArgs[i].toLowerCase().equals("verseschema=restrictkjv")) {
				verseSchema = -2;
			} else if (exportArgs[i].toLowerCase().startsWith("verseschema=")) {
				String[] params = exportArgs[i].substring(12).split("@", 2);
				versification = new VersificationSet(new File(params[1])).findVersification(params[0]);
				verseSchema = 2;
			} else if (exportArgs[i].toLowerCase().startsWith("encoding=")) {
				encodings = exportArgs[i].substring(9).split(",");
			} else {
				parseFormatRule(exportArgs[i], formatRules);
			}
		}
		try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(mainFile), StandardCharsets.UTF_8));
				BufferedWriter bnw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(booknameFile), StandardCharsets.UTF_8))) {
			for (Book book : bible.getBooks()) {
				String bookName = BOOK_NAME_MAP.get(book.getId());
				if (bookName == null) {
					System.out.println("WARNING: Skipping book " + book.getAbbr());
					continue;
				}
				int cnumber = 0;
				List<Chapter> chapters = book.getChapters();
				List<List<Integer>> allReferences = new ArrayList<>();
				if (verseSchema == 2) {
					for (int i = 0; i < versification.getVerseCount(); i++) {
						Reference r = versification.getReference(i);
						if (r.getBook() != book.getId())
							continue;
						int chapter = r.getChapter();
						String verse = r.getVerse();
						if (chapter == 1 && verse.endsWith("/p")) {
							chapter = 0;
							verse = verse.substring(0, verse.length() - 2);
						}
						if (verse.equals("1/t"))
							verse = "0";
						if (!verse.matches("[0-9]+"))
							throw new IOException("Unsupported verse reference in versification: " + verse);
						while (chapter >= allReferences.size())
							allReferences.add(new ArrayList<>());
						allReferences.get(chapter).add(Integer.parseInt(verse));
					}
					if (allReferences.isEmpty()) {
						System.out.println("WARNING: Skipping book " + book.getAbbr() + " as it is not contained in versification");
						continue;
					}
					if (!allReferences.get(0).isEmpty()) {
						// we have chapter zero here, so we need to split
						// verses!
						cnumber--;
						chapters = new ArrayList<>(chapters);
						Chapter chapter0 = new Chapter();
						Chapter chapter1 = new Chapter();
						Chapter origChapter = chapters.remove(0);
						for (Verse v : origChapter.getVerses()) {
							if (v.getNumber().endsWith("/p")) {
								Verse vv = new Verse(v.getNumber().substring(0, v.getNumber().length() - 2));
								v.accept(vv.getAppendVisitor());
								vv.finished();
								chapter0.getVerses().add(vv);
							} else {
								chapter1.getVerses().add(v);
							}
						}
						if (chapter0.getVerses().isEmpty())
							chapter1.setProlog(origChapter.getProlog());
						else
							chapter0.setProlog(origChapter.getProlog());
						chapters.add(0, chapter1);
						chapters.add(0, chapter0);
					}
					int allowedChapters = allReferences.size() - cnumber - 1;
					if (chapters.size() > allowedChapters) {
						if (cnumber == 0)
							chapters = new ArrayList<>(chapters);
						Chapter lastAllowedChapter = new Chapter();
						lastAllowedChapter.setProlog(chapters.get(allowedChapters - 1).getProlog());
						lastAllowedChapter.getVerses().addAll(chapters.get(allowedChapters - 1).getVerses());
						for (int ch = allowedChapters; ch < chapters.size(); ch++) {
							int cnum = ch + cnumber + 1;
							for (Verse v : chapters.get(ch).getVerses()) {
								Verse vv = new Verse(cnum + "," + v.getNumber());
								v.accept(vv.getAppendVisitor());
								vv.finished();
								lastAllowedChapter.getVerses().add(vv);
							}
						}
						while (chapters.size() >= allowedChapters)
							chapters.remove(chapters.size() - 1);
						chapters.add(lastAllowedChapter);
					}
				}
				bnw.write(bookName.replace(".", "") + "\t" + book.getAbbr().replace(".", "") + lineEnding);
				bw.write(bookName + " ");
				for (Chapter chapter : chapters) {
					FormattedText prolog = chapter.getProlog();
					cnumber++;
					List<VirtualVerse> vvs;
					Map<String, String> verseNumberMap = null;
					int nextFillVerse = verseSchema < 0 ? 99999 : verseSchema, lastFillVerse = -1;
					if (nextFillVerse == 0 && !(book.getId() == BookID.BOOK_Ps && psalmSet.get(cnumber)))
						nextFillVerse = 1;
					if (verseSchema == -2) {
						int[] verseCounts = StandardVersification.KJV.getVerseCount(book.getId());
						int minVerse = book.getId() == BookID.BOOK_Ps && psalmSet.get(cnumber) ? 0 : 1;
						int maxVerse = verseCounts != null && cnumber <= verseCounts.length ? verseCounts[cnumber - 1] : -1;
						BitSet verseBits = maxVerse == -1 ? null : new BitSet(maxVerse + 1);
						if (verseBits != null)
							verseBits.set(minVerse, maxVerse + 1);
						vvs = chapter.createVirtualVerses(true, verseBits, false);
					} else if (verseSchema == 2) {
						Chapter dummyChapter = new Chapter();
						verseNumberMap = new HashMap<>();
						if (cnumber >= allReferences.size() || allReferences.get(cnumber).isEmpty()) {
							System.out.println("WARNING: Skipping export of " + book.getAbbr() + " " + cnumber + " as it is not contained in versification!");
							continue;
						}
						List<Integer> references = allReferences.get(cnumber);
						for (int i = 0; i < references.size(); i++) {
							verseNumberMap.put("" + (i + 1), "" + references.get(i));
						}
						int nextExtraVerse = 1, maxSeenRealVerse = -2;
						for (Verse v : chapter.getVerses()) {
							String vnumber = v.getNumber().equals("1/t") ? "0" : v.getNumber();
							int idx;
							try {
								int vnum = Integer.parseInt(vnumber);
								maxSeenRealVerse = Math.max(vnum, maxSeenRealVerse);
								idx = references.indexOf(vnum);
							} catch (NumberFormatException ex) {
								idx = -1;
							}
							String newNum = "" + (idx + 1);
							if (idx == -1) {
								if (vnumber.startsWith("" + (maxSeenRealVerse + 1)) && references.contains(maxSeenRealVerse + 1)) {
									maxSeenRealVerse++;
									Verse dv = new Verse("" + maxSeenRealVerse);
									dv.finished();
									dummyChapter.getVerses().add(dv);
								}
								newNum = nextExtraVerse + "x";
								nextExtraVerse++;
								verseNumberMap.put(newNum, vnumber);
							}
							Verse vv = new Verse(newNum);
							v.accept(vv.getAppendVisitor());
							vv.finished();
							dummyChapter.getVerses().add(vv);
						}
						nextFillVerse = 1;
						lastFillVerse = references.size();
						BitSet verseBits = new BitSet(lastFillVerse + 1);
						verseBits.set(1, lastFillVerse + 1);
						vvs = dummyChapter.createVirtualVerses(false, verseBits, false);
					} else {
						vvs = chapter.createVirtualVerses(true, false);
					}
					bw.write(cnumber + ":");
					if (vvs.isEmpty()) {
						if (verseSchema < 0) {
							bw.write("1 " + (paraMarker ? "¶" : "") + lineEnding);
						} else {
							bw.write(mapBack(verseNumberMap, "" + nextFillVerse) + " " + (paraMarker ? "¶ " : "") + "-" + lineEnding);
							nextFillVerse++;
						}
						paraMarker = false;
					}
					for (VirtualVerse vv : vvs) {
						while (nextFillVerse < vv.getNumber()) {
							bw.write(mapBack(verseNumberMap, "" + nextFillVerse) + " " + (paraMarker ? "¶ " : "") + "-" + lineEnding);
							nextFillVerse++;
						}
						bw.write(mapBack(verseNumberMap, "" + vv.getNumber()) + " " + (paraMarker ? "¶ " : ""));
						if (nextFillVerse == vv.getNumber())
							nextFillVerse++;
						paraMarker = false;
						AccordanceVisitor av = new AccordanceVisitor(formatRules, unformattedElements);
						av.start();
						if (prolog != null) {
							AccordanceVisitor plv = av.startProlog();
							if (plv != null) {
								prolog.accept(plv);
							}
							prolog = null;
						}
						if (!vv.getHeadlines().isEmpty())
							throw new IllegalStateException();
						av.visitEnd();
						boolean firstVerse = true;
						for (Verse v : vv.getVerses()) {
							av.start();
							if (!firstVerse || !v.getNumber().equals(vv.getNumber() == 0 ? "1/t" : "" + vv.getNumber())) {
								av.visitText(" ");
								Visitor<RuntimeException> nv = av.visitElement("VN", DEFAULT_VERSENO);
								if (nv != null) {
									nv.visitText(mapBack(verseNumberMap, v.getNumber()));
									nv.visitEnd();
								}
								av.visitText(" ");
							}
							v.accept(av);
							firstVerse = false;
						}
						String verseText = av.getContent().replaceAll("  +", " ").trim();
						if (verseText.endsWith("¶")) {
							verseText = verseText.substring(0, verseText.length() - 1);
							paraMarker = true;
						}
						bw.write(verseText + lineEnding);
					}
					while (nextFillVerse <= lastFillVerse) {
						bw.write(mapBack(verseNumberMap, "" + nextFillVerse) + " " + (paraMarker ? "¶ " : "") + "-" + lineEnding);
						nextFillVerse++;
					}
				}
			}
		}
		if (mainFile.length() > 0) {
			try (RandomAccessFile raf = new RandomAccessFile(mainFile, "rw")) {
				raf.setLength(mainFile.length() - 1);
			}
		}
		if (encodings != null) {
			String content = new String(Files.readAllBytes(mainFile.toPath()), StandardCharsets.UTF_8);
			String booknameContent = new String(Files.readAllBytes(booknameFile.toPath()), StandardCharsets.UTF_8);
			byte[] bytes = null, booknameBytes = null;
			;
			for (String encoding : encodings) {
				byte[] trying = content.getBytes(encoding);
				byte[] booknameTrying = booknameContent.getBytes(encoding);
				if (new String(trying, encoding).equals(content) && new String(booknameTrying, encoding).equals(booknameContent)) {
					bytes = trying;
					booknameBytes = booknameTrying;
					break;
				}
			}
			if (bytes == null) {
				System.out.println("WARNING: All encoding could not losslessly encode the bible, using " + encodings[encodings.length - 1] + " anyway");
				bytes = content.getBytes(encodings[encodings.length - 1]);
				booknameBytes = booknameContent.getBytes(encodings[encodings.length - 1]);
			}
			try (FileOutputStream fos = new FileOutputStream(mainFile)) {
				fos.write(bytes);
			}
			try (FileOutputStream fos = new FileOutputStream(booknameFile)) {
				fos.write(booknameBytes);
			}
		}
		if (!unformattedElements.isEmpty())
			System.out.println("WARNING: No formatting specified for elements: " + unformattedElements);
	}

	private String mapBack(Map<String, String> verseNumberMap, String verseNumber) {
		if (verseNumberMap == null)
			return verseNumber;
		return verseNumberMap.get(verseNumber).toString();
	}

	private void parseFormatRule(String rule, Map<String, String[]> formatRules) {
		String[] parts = rule.toUpperCase().split("=");
		if (parts.length != 2)
			throw new RuntimeException("Unsupported format rule: " + rule);
		if (parts[0].equals("H*") || parts[0].equals("PL:H*")) {
			for (int i = 1; i <= 9; i++)
				parseFormatRule(parts[0].replace('*', (char) ('0' + i)) + "=" + parts[1], formatRules);
			return;
		}
		if (!parts[0].startsWith("CSS:") && !parts[0].startsWith("PL:CSS:") && !SUPPORTED_ELEMENTS.contains(parts[0]))
			throw new RuntimeException("Unsupported element in format rule: " + rule);
		if (parts[1].equals("-")) {
			formatRules.put(parts[0], new String[0]);
		} else if (parts[1].contains("#")) {
			String[] formats = parts[1].split("#");
			if (formats.length != 2)
				throw new RuntimeException("Unsupported formatting in format rule: " + rule);
			String[] f1 = parseFormats(formats[0]), f2 = parseFormats(formats[1]);
			formatRules.put(parts[0], new String[] { f1[0], f1[1], f2[0], f2[1] });
		} else {
			formatRules.put(parts[0], parseFormats(parts[1]));
		}
	}

	private String[] parseFormats(String formats) {
		if (formats.equals("+"))
			return new String[] { "", "" };
		StringBuilder prefix = new StringBuilder(), suffix = new StringBuilder();
		for (String format : formats.split("\\+")) {
			Format fmt = Format.valueOf(format);
			prefix.append(fmt.prefix);
			suffix.insert(0, fmt.suffix);
		}
		return new String[] { prefix.toString(), suffix.toString() };
	}

	private static class AccordanceVisitor implements Visitor<RuntimeException> {

		private TagReorderStringBuilder sb = new TagReorderStringBuilder(PendingLineBreak.NOBREAK), fnb = new TagReorderStringBuilder(PendingLineBreak.NONE);
		private boolean inFootnote = false;
		private int footnoteNumber = 0;
		private final List<String> suffixStack = new ArrayList<String>();
		private final Map<String, String[]> formatRules;
		private final Set<String> unspecifiedFormattings;
		private String elementPrefix = "";

		public AccordanceVisitor(Map<String, String[]> formatRules, Set<String> unspecifiedFormattings) {
			this.formatRules = formatRules;
			this.unspecifiedFormattings = unspecifiedFormattings;
		}

		public void start() {
			if (inFootnote || !suffixStack.isEmpty())
				throw new IllegalStateException();
			pushSuffix("");
		}

		public AccordanceVisitor startProlog() {
			if (!elementPrefix.isEmpty())
				throw new IllegalStateException();
			AccordanceVisitor next = visitElement("PL", DEFAULT_SKIP);
			if (next != null) {
				pushSuffix("\0\0\1");
				elementPrefix = "PL:";
			}
			return next;
		}

		private String getContent() {
			if (inFootnote || !suffixStack.isEmpty())
				throw new IllegalStateException();
			return sb.getContent() + fnb.getContent() + sb.pendingLineBreak.merge(fnb.pendingLineBreak).getText();
		}

		private void pushSuffix(String suffix) {
			suffixStack.add(suffix);
		}

		private String[] getElementRule(String elementType, String[] defaultRule) {
			String[] rule = formatRules.get(elementPrefix + elementType);
			if (rule == null) {
				unspecifiedFormattings.add(elementPrefix + elementType);
				rule = defaultRule;
			}
			return rule;
		}

		private AccordanceVisitor visitElement(String elementType, String[] defaultRule) {
			String[] rule = getElementRule(elementType, defaultRule);
			if (rule.length == 0) {
				return null;
			} else if (rule.length == 2) {
				sb.append(rule[0]);
				pushSuffix(rule[1]);
				return this;
			} else if (rule.length == 4) {
				if (inFootnote)
					throw new IllegalStateException("Footnote inside footnote");
				inFootnote = true;
				footnoteNumber++;
				sb.append(rule[0] + footnoteNumber);
				fnb.append(" " + rule[2] + footnoteNumber + " ");
				pushSuffix(rule[1] + "\0" + rule[3]);
				TagReorderStringBuilder tmp = sb;
				sb = fnb;
				fnb = tmp;
				pushSuffix("\0\0\2");
				return this;
			} else {
				throw new IllegalStateException();
			}
		}

		private void appendRule(String[] suffixes, String[] rule, String text) {
			if (rule.length == 2) {
				suffixes[0] += " " + rule[0] + text + rule[1];
			} else if (rule.length == 4) {
				footnoteNumber++;
				suffixes[0] += " " + rule[0] + footnoteNumber + rule[1];
				suffixes[1] += " " + rule[2] + footnoteNumber + " " + text + rule[3];
			} else {
				throw new IllegalStateException();
			}
		}

		@Override
		public void visitVerseSeparator() {
			sb.appendText("/");
		}

		@Override
		public int visitElementTypes(String elementTypes) {
			return 0;
		}

		@Override
		public void visitStart() {
		}

		@Override
		public void visitText(String text) {
			sb.appendText(text.replace("<", "〈").replace(">", "〉").replace("\u00A0", " "));
		}

		@Override
		public Visitor<RuntimeException> visitCSSFormatting(String css) {
			return visitElement("CSS:" + css.replace(" ", "_").toUpperCase(), DEFAULT_KEEP);
		}

		@Override
		public Visitor<RuntimeException> visitFormattingInstruction(FormattingInstructionKind kind) {
			return visitElement(("" + kind.getCode()).toUpperCase(), DEFAULT_KEEP);
		}

		@Override
		public Visitor<RuntimeException> visitHeadline(int depth) {
			return visitElement("H" + depth, DEFAULT_SKIP);
		}

		@Override
		public Visitor<RuntimeException> visitFootnote() {
			return visitElement("FN", DEFAULT_SKIP);
		}

		@Override
		public Visitor<RuntimeException> visitCrossReference(String bookAbbr, BookID book, int firstChapter, String firstVerse, int lastChapter, String lastVerse) {
			return visitElement("XREF", DEFAULT_KEEP);
		}

		@Override
		public void visitLineBreak(LineBreakKind kind) {
			switch (kind) {
			case NEWLINE:
			case NEWLINE_WITH_INDENT:
				sb.append("<br>");
				return;
			case PARAGRAPH:
				sb.append("<para>");
				break;
			}
		}

		@Override
		public Visitor<RuntimeException> visitGrammarInformation(int[] strongs, String[] rmac, int[] sourceIndices) {
			Visitor<RuntimeException> next = visitElement("GRAMMAR", DEFAULT_KEEP);
			if (next == null)
				return null;
			String[] suffixes = { suffixStack.remove(suffixStack.size() - 1), "" };
			if (strongs != null) {
				String[] rule = getElementRule("STRONG", DEFAULT_SKIP);
				if (rule.length > 0) {
					StringBuilder sb = new StringBuilder();
					for (int strong : strongs) {
						sb.append(" G").append(strong);
					}
					appendRule(suffixes, rule, sb.toString().trim());
				}
			}
			if (rmac != null) {
				String[] rule = getElementRule("MORPH", DEFAULT_SKIP);
				if (rule.length > 0) {
					StringBuilder sb = new StringBuilder();
					for (String morph : rmac) {
						sb.append(" ").append(morph);
					}
					appendRule(suffixes, rule, sb.toString().trim());
				}
			}
			if (!suffixes[1].isEmpty())
				suffixes[0] += "\0" + suffixes[1];
			pushSuffix(suffixes[0]);
			return next;
		}

		@Override
		public Visitor<RuntimeException> visitDictionaryEntry(String dictionary, String entry) {
			return visitElement("DICT", DEFAULT_KEEP);
		}

		@Override
		public void visitRawHTML(RawHTMLMode mode, String raw) {
			unspecifiedFormattings.add(elementPrefix + "RAWHTML");
		}

		@Override
		public Visitor<RuntimeException> visitVariationText(String[] variations) {
			throw new UnsupportedOperationException("Variation text not supported");
		}

		@Override
		public Visitor<RuntimeException> visitExtraAttribute(ExtraAttributePriority prio, String category, String key, String value) {
			Visitor<RuntimeException> next = prio.handleVisitor(category, this);
			if (next != null)
				pushSuffix("");
			return next;
		}

		@Override
		public boolean visitEnd() {
			String suffix = suffixStack.remove(suffixStack.size() - 1);
			if (suffix.equals("\0\0\1")) {
				elementPrefix = "";
				suffix = suffixStack.remove(suffixStack.size() - 1);
			}
			if (suffix.equals("\0\0\2")) {
				if (!inFootnote)
					throw new IllegalStateException();
				inFootnote = false;
				TagReorderStringBuilder tmp = sb;
				sb = fnb;
				fnb = tmp;
				suffix = suffixStack.remove(suffixStack.size() - 1);
			}
			if (suffix.contains("\0")) {
				String[] sparts = suffix.split("\0");
				if (sparts.length != 2)
					throw new IllegalStateException();
				sb.append(sparts[0]);
				fnb.append(sparts[1]);
			} else {
				sb.append(suffix);
			}
			return false;
		}
	}

	private static class TagReorderStringBuilder {
		private final StringBuilder sb = new StringBuilder();
		private final List<ShadowTag> shadowTagStack = new ArrayList<>();
		private PendingLineBreak pendingLineBreak;

		public TagReorderStringBuilder(PendingLineBreak pendingLineBreak) {
			this.pendingLineBreak = pendingLineBreak;
		}

		public void appendText(String text) {
			if (text.isEmpty())
				return;
			sb.append(pendingLineBreak.getText());
			pendingLineBreak = PendingLineBreak.NONE;
			for (ShadowTag t : shadowTagStack) {
				if (t.printed)
					continue;
				sb.append(t.getOpenTag());
				t.printed = true;
			}
			sb.append(text);
		}

		public void append(String textWithTags) {
			int start = 0, pos = textWithTags.indexOf('<');
			while (pos != -1) {
				appendText(textWithTags.substring(start, pos));
				int endPos = textWithTags.indexOf('>', pos);
				if (endPos == -1)
					throw new RuntimeException("Incomplete tag in " + textWithTags);
				String[] tagInfo = textWithTags.substring(pos + 1, endPos).split("=", 2);
				if (tagInfo.length == 1 && tagInfo[0].equals("br")) {
					pendingLineBreak = pendingLineBreak.merge(PendingLineBreak.BR);
				} else if (tagInfo.length == 1 && tagInfo[0].equals("para")) {
					pendingLineBreak = pendingLineBreak.merge(PendingLineBreak.PARAGRAPH);
				} else if (tagInfo.length == 1 && tagInfo[0].equals("nobreak")) {
					pendingLineBreak = pendingLineBreak.merge(PendingLineBreak.NOBREAK);
				} else if (tagInfo.length == 1 && tagInfo[0].startsWith("/")) {
					ShadowTag lastTag = shadowTagStack.remove(shadowTagStack.size() - 1);
					if (!lastTag.name.equals(tagInfo[0].substring(1)))
						throw new RuntimeException("Closing tag <" + tagInfo[0] + "> does not match expected tag " + lastTag.name);
					if (lastTag.shadowed)
						throw new IllegalStateException("Closing a tag that was not unshadowed");
					sb.append(lastTag.getCloseTagIfPrinted());
					if (lastTag.type == ShadowTagType.SHADOWING) {
						int shadowedIndex = -1;
						for (int i = shadowTagStack.size() - 1; i >= 0; i--) {
							ShadowTag tag = shadowTagStack.get(i);
							sb.append(tag.getCloseTagIfPrinted());
							tag.printed = false;
							if (tag.shadowed && tag.name.equals(lastTag.name)) {
								tag.shadowed = false;
								shadowedIndex = i;
								break;
							}
						}
						if (shadowedIndex == -1)
							throw new IllegalStateException("Shadow stack mismatch");
					}
				} else {
					int shadowCandidateIndex = -1;
					for (int i = shadowTagStack.size() - 1; i >= 0; i--) {
						ShadowTag tag = shadowTagStack.get(i);
						if (tag.type != ShadowTagType.REDUNDANT && !tag.shadowed && tag.name.equals(tagInfo[0])) {
							shadowCandidateIndex = i;
							break;
						}
					}
					ShadowTagType newType = ShadowTagType.NORMAL;
					String tagValue = tagInfo.length == 2 ? tagInfo[1] : "";
					if (shadowCandidateIndex != -1) {
						ShadowTag candidate = shadowTagStack.get(shadowCandidateIndex);
						if (candidate.value.equals(tagValue)) {
							newType = ShadowTagType.REDUNDANT;
						} else {
							newType = ShadowTagType.SHADOWING;
							for (int i = shadowTagStack.size() - 1; i >= shadowCandidateIndex; i--) {
								ShadowTag tag = shadowTagStack.get(i);
								sb.append(shadowTagStack.get(i).getCloseTagIfPrinted());
								tag.printed = false;
							}
							candidate.shadowed = true;
						}
					}
					ShadowTag newTag = new ShadowTag(newType, tagInfo[0], tagValue);
					shadowTagStack.add(newTag);
				}
				start = endPos + 1;
				pos = textWithTags.indexOf('<', start);
			}
			appendText(textWithTags.substring(start));
		}

		public String getContent() {
			if (!shadowTagStack.isEmpty())
				throw new IllegalStateException();
			return sb.toString();
		}
	}

	private static class ShadowTag {
		private final ShadowTagType type;
		private final String name;
		private final String value;
		private boolean shadowed = false, printed = false;

		private ShadowTag(ShadowTagType type, String name, String value) {
			this.type = type;
			this.name = name;
			this.value = value;
		}

		private String getOpenTag() {
			if (shadowed || type == ShadowTagType.REDUNDANT)
				return "";
			return "<" + name + (value.isEmpty() ? "" : "=" + value) + ">";
		}

		private String getCloseTagIfPrinted() {
			if (shadowed || !printed || type == ShadowTagType.REDUNDANT)
				return "";
			return "</" + name + ">";
		}
	}

	private static enum ShadowTagType {
		NORMAL, SHADOWING, REDUNDANT
	}

	private static enum PendingLineBreak {
		NONE(""), BR("<br>"), PARAGRAPH("¶"), NOBREAK("");

		private String text;

		private PendingLineBreak(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}

		public PendingLineBreak merge(PendingLineBreak other) {
			return values()[Math.max(ordinal(), other.ordinal())];
		}
	}

	private static final String[] DEFAULT_KEEP = { "", "" }, DEFAULT_SKIP = {}, DEFAULT_VERSENO = { "<color=teal>(", ")</color>" };

	private static enum Format {
		PARENS("(", ")"), BRACKETS("[", "]"), BRACES("{", "}"), NOBREAK("<nobreak>", "<nobreak>"), //
		BR_START("<br>", ""), PARA_START("<para>", ""), BR_END("", "<br>"), PARA_END("", "<para>"), //
		BOLD("<b>", "</b>"), SMALL_CAPS("<c>", "</c>"), ITALIC("<i>", "</i>"), SUB("<sub>", "</sub>"), SUP("<sup>", "</sup>"), UNDERLINE("<u>", "</u>"), //

		BLACK("<color=black>", "</color>"), GRAY("<color=gray>", "</color>"), WHITE("<color=white>", "</color>"), CHOCOLATE("<color=chocolate>", "</color>"), //
		BURGUNDY("<color=burgundy>", "</color>"), RED("<color=red>", "</color>"), ORANGE("<color=orange>", "</color>"), BROWN("<color=brown>", "</color>"), //
		YELLOW("<color=yellow>", "</color>"), CYAN("<color=cyan>", "</color>"), TURQUOISE("<color=turquoise>", "</color>"), GREEN("<color=green>", "</color>"), //
		OLIVE("<color=olive>", "</color>"), FOREST("<color=forest>", "</color>"), TEAL("<color=teal>", "</color>"), SAPPHIRE("<color=sapphire>", "</color>"), //
		BLUE("<color=blue>", "</color>"), NAVY("<color=navy>", "</color>"), PURPLE("<color=purple>", "</color>"), LAVENDER("<color=lavender>", "</color>"), //
		MAGENTA("<color=magenta>", "</color>");

		private final String prefix, suffix;

		private Format(String prefix, String suffix) {
			this.prefix = prefix;
			this.suffix = suffix;
		}
	}
}
