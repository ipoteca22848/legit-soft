package com.example.auravisuals.util.render.gui;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class GifDecoder {
    GifDecoder() {
    }

    public static GifDecoder$Result read(Path file) throws Exception {
        try (
            InputStream in = Files.newInputStream(file);
            ImageInputStream stream = ImageIO.createImageInputStream(in);
        ) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) {
                return null;
            }

            ImageReader reader = readers.next();
            reader.setInput(stream);
            int numImages = reader.getNumImages(true);
            if (numImages <= 0) {
                reader.dispose();
                return null;
            }

            int canvasW = 0;
            int canvasH = 0;
            int[] xs = new int[numImages];
            int[] ys = new int[numImages];
            int[] delays = new int[numImages];
            String[] disposals = new String[numImages];
            List<BufferedImage> rawFrames = new ArrayList<>(numImages);

            for (int i = 0; i < numImages; i++) {
                BufferedImage raw = reader.read(i);
                rawFrames.add(raw);
                if (canvasW <= 0) {
                    canvasW = raw.getWidth();
                    canvasH = raw.getHeight();
                }

                delays[i] = 40;
                disposals[i] = "none";

                try {
                    IIOMetadata meta = reader.getImageMetadata(i);

                    for (String format : meta.getMetadataFormatNames()) {
                        Node root = meta.getAsTree(format);
                        delays[i] = extractDelay(root, delays[i]);
                        disposals[i] = extractDisposal(root, disposals[i]);
                        xs[i] = extractAttrInt(root, "imageLeftPosition", xs[i]);
                        ys[i] = extractAttrInt(root, "imageTopPosition", ys[i]);
                    }
                } catch (Throwable var25) {
                }
            }

            reader.dispose();
            if (!rawFrames.isEmpty()) {
                List<BufferedImage> composedFrames = new ArrayList<>(numImages);
                List<Integer> finalDelays = new ArrayList<>(numImages);
                BufferedImage master = new BufferedImage(canvasW, canvasH, 2);
                BufferedImage prevMaster = null;

                for (int i = 0; i < numImages; i++) {
                    BufferedImage raw = rawFrames.get(i);
                    String disp = disposals[i];
                    if ("restoreToPrevious".equalsIgnoreCase(disp) && prevMaster != null) {
                        master = copy(prevMaster);
                    } else {
                        prevMaster = copy(master);
                    }

                    Graphics2D g = master.createGraphics();
                    g.drawImage(raw, xs[i], ys[i], null);
                    g.dispose();
                    composedFrames.add(copy(master));
                    finalDelays.add(Math.max(20, delays[i]));
                    if ("restoreToBackgroundColor".equalsIgnoreCase(disp)) {
                        Graphics2D cg = master.createGraphics();
                        cg.setComposite(AlphaComposite.Clear);
                        cg.fillRect(xs[i], ys[i], raw.getWidth(), raw.getHeight());
                        cg.dispose();
                    }
                }

                int[] dArr = new int[finalDelays.size()];

                for (int i = 0; i < dArr.length; i++) {
                    dArr[i] = finalDelays.get(i);
                }

                return new GifDecoder$Result(composedFrames, dArr);
            } else {
                return null;
            }
        }
    }

    static BufferedImage copy(BufferedImage src) {
        BufferedImage dest = new BufferedImage(src.getWidth(), src.getHeight(), 2);
        Graphics2D g = dest.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dest;
    }

    static int extractDelay(Node node, int defaultVal) {
        if (node == null) {
            return defaultVal;
        }

        if ("GraphicControlExtension".equalsIgnoreCase(node.getNodeName())) {
            NamedNodeMap attr = node.getAttributes();
            if (attr != null) {
                Node delayNode = attr.getNamedItem("delayTime");
                if (delayNode != null) {
                    try {
                        int delayHundreds = Integer.parseInt(delayNode.getNodeValue());
                        return delayHundreds * 10;
                    } catch (Exception var5) {
                    }
                }
            }
        }

        NodeList children = node.getChildNodes();
        if (children != null) {
            for (int i = 0; i < children.getLength(); i++) {
                int val = extractDelay(children.item(i), defaultVal);
                if (val != defaultVal) {
                    return val;
                }
            }
        }

        return defaultVal;
    }

    static String extractDisposal(Node node, String defaultVal) {
        if (node == null) {
            return defaultVal;
        }

        if ("GraphicControlExtension".equalsIgnoreCase(node.getNodeName())) {
            NamedNodeMap attr = node.getAttributes();
            if (attr != null) {
                Node dNode = attr.getNamedItem("disposalMethod");
                if (dNode != null) {
                    return dNode.getNodeValue();
                }
            }
        }

        NodeList children = node.getChildNodes();
        if (children != null) {
            for (int i = 0; i < children.getLength(); i++) {
                String val = extractDisposal(children.item(i), defaultVal);
                if (val != null && !val.equals(defaultVal)) {
                    return val;
                }
            }
        }

        return defaultVal;
    }

    static int extractAttrInt(Node node, String attrName, int defaultVal) {
        if (node == null) {
            return defaultVal;
        }

        NamedNodeMap attr = node.getAttributes();
        if (attr != null) {
            Node n = attr.getNamedItem(attrName);
            if (n != null) {
                try {
                    return Integer.parseInt(n.getNodeValue());
                } catch (Exception var7) {
                }
            }
        }

        NodeList children = node.getChildNodes();
        if (children != null) {
            for (int i = 0; i < children.getLength(); i++) {
                int val = extractAttrInt(children.item(i), attrName, defaultVal);
                if (val != defaultVal) {
                    return val;
                }
            }
        }

        return defaultVal;
    }
}
