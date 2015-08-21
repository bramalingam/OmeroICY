/*
 *------------------------------------------------------------------------------
 *  Copyright (C) 2014 University of Dundee. All rights reserved.
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */
package plugins.bramalingam.OMEROICY;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import icy.gui.dialog.MessageDialog;

import icy.roi.ROI2D;
import ij.IJ;
import loci.formats.in.DefaultMetadataOptions;
import loci.formats.in.MetadataLevel;
import loci.plugins.util.LibraryChecker;
import ome.formats.OMEROMetadataStoreClient;
import ome.formats.importer.ImportCandidates;
import ome.formats.importer.ImportConfig;
import ome.formats.importer.ImportLibrary;
import ome.formats.importer.OMEROWrapper;
import ome.formats.importer.cli.ErrorHandler;
import ome.formats.importer.cli.LoggingImportMonitor;
import omero.RDouble;
import omero.RInt;
import omero.RString;
import omero.ServerError;
import omero.client;
import omero.api.IContainerPrx;
import omero.api.IUpdatePrx;
import omero.api.RawFileStorePrx;
import omero.api.ServiceFactoryPrx;
import omero.model.Dataset;
import omero.model.Ellipse;
import omero.model.EllipseI;
import omero.model.FileAnnotation;
import omero.model.FileAnnotationI;
import omero.model.IObject;
import omero.model.Image;
import omero.model.ImageAnnotationLink;
import omero.model.ImageAnnotationLinkI;
import omero.model.Line;
import omero.model.LineI;
import omero.model.OriginalFile;
import omero.model.OriginalFileI;
import omero.model.Point;
import omero.model.PointI;
import omero.model.Polygon;
import omero.model.PolygonI;
import omero.model.Polyline;
import omero.model.PolylineI;
import omero.model.Project;
import omero.model.Rect;
import omero.model.RectI;
import omero.model.Roi;
import omero.model.RoiI;
import omero.sys.ParametersI;
import plugins.kernel.roi.roi2d.ROI2DArea;
import plugins.kernel.roi.roi2d.ROI2DEllipse;
import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DPoint;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DRectangle;
import plugins.kernel.roi.roi2d.ROI2DShape;
import pojos.DatasetData;
import pojos.ImageData;
import Glacier2.CannotCreateSessionException;
import Glacier2.PermissionDeniedException;

/**
 * 
 *
 * @author Balaji Ramalingam &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:b.ramalingam@dundee.ac.uk">b.ramalingam@dundee.ac.uk</a>
 * @since 5.1
 */
public class testOmero {

    public static client omeroLogin(String hostName,int port,String userName, String password){
        client client = new client(hostName, port);

        try {
            ServiceFactoryPrx entry = client.createSession(userName, password);
            client.enableKeepAlive(60);
        } catch (CannotCreateSessionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (PermissionDeniedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ServerError e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return client;

    }

    public static void uploadImage(String hostName,int port,String userName, String password,long datasetID, String[] paths){

        loci.common.DebugTools.enableLogging("DEBUG");
        ImportConfig config = new ome.formats.importer.ImportConfig();

        config.email.set("");
        config.sendFiles.set(true);
        config.sendReport.set(false);
        config.contOnError.set(false);
        config.debug.set(false);

        config.hostname.set(hostName);
        config.port.set(port);
        config.username.set(userName);
        config.password.set(password);
        config.targetClass.set("omero.model.Dataset");

        config.targetId.set(datasetID);

        OMEROMetadataStoreClient store;
        try {
            store = config.createStore();
            store.logVersionInfo(config.getIniVersionNumber());
            OMEROWrapper reader = new OMEROWrapper(config);
            ImportLibrary library = new ImportLibrary(store, reader);

            ErrorHandler handler = new ErrorHandler(config);
            library.addObserver(new LoggingImportMonitor());

            ImportCandidates candidates = new ImportCandidates(reader, paths, handler);
            reader.setMetadataOptions(new DefaultMetadataOptions(MetadataLevel.ALL));
            library.importCandidates(config, candidates);

            store.logout();
            MessageDialog.showDialog("Success");

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void openOmeroImage(String hostName,String port,String userName, String password , String groupId , String imageId){
        if (!LibraryChecker.checkImageJ()) return;
        StringBuffer buffer = new StringBuffer();

        buffer.append("location=[OMERO] open=[omero:server=");
        buffer.append(hostName);
        buffer.append("\nuser=");
        buffer.append(userName);
        buffer.append("\nport=");
        buffer.append(port);
        buffer.append("\npass=");
        buffer.append(password);
        buffer.append("\ngroupID=");
        buffer.append(groupId);
        buffer.append("\niid=");
        buffer.append(imageId);
        buffer.append("]");
        buffer.append(" windowless=true ");

        IJ.runPlugIn("loci.plugins.LociImporter", buffer.toString());

    }

    public static List<IObject> getProjects(ServiceFactoryPrx entryUnencrypted) throws ServerError{
        IContainerPrx proxy = entryUnencrypted.getContainerService();
        ParametersI param = new ParametersI();
        long userId = entryUnencrypted.getAdminService().getEventContext().userId;
        param.exp(omero.rtypes.rlong(userId));
        param.noLeaves();
        List<IObject> results = proxy.loadContainerHierarchy(
                Project.class.getName(), new ArrayList<Long>(), param);
        return results;
    }

    public static List<IObject> getDatasets(ServiceFactoryPrx entryUnencrypted) throws ServerError{
        IContainerPrx proxy = entryUnencrypted.getContainerService();
        ParametersI param = new ParametersI();
        long userId = entryUnencrypted.getAdminService().getEventContext().userId;
        param.exp(omero.rtypes.rlong(userId));

        param.noLeaves();
        List<IObject> results = proxy.loadContainerHierarchy(Dataset.class.getName(), new ArrayList<Long>(), param);
        return results;
    }

    public static Set<ImageData> getImages(Dataset Dataset){
        DatasetData dataset = new DatasetData(Dataset);
        Set<ImageData> images = dataset.getImages();
        Iterator<ImageData> j = images.iterator();
        ImageData image;
        return images;
    }

    public static void setFileAnnotation(String fileToUpload,long ImageId,ServiceFactoryPrx entryUnencrypted,String ICY_NameSpace) throws ServerError, IOException{

        List<Image> results = entryUnencrypted.getContainerService().getImages(Image.class.getName(), Arrays.asList(ImageId), new ParametersI());
        ImageData image = new ImageData(results.get(0));
        File file = new File(fileToUpload);
        String name = file.getName();
        String absolutePath = file.getAbsolutePath();
        String path = absolutePath.substring(0,
                absolutePath.length()-name.length());

        IUpdatePrx iUpdate = entryUnencrypted.getUpdateService(); // service used to write object
        // create the original file object.
        OriginalFile originalFile = new OriginalFileI();
        originalFile.setName(omero.rtypes.rstring(name));
        originalFile.setPath(omero.rtypes.rstring(path));
        originalFile.setSize(omero.rtypes.rlong(file.length()));
        originalFile.setHasher(new omero.model.ChecksumAlgorithmI());
        originalFile.getHasher().setValue(omero.rtypes.rstring("SHA1-160"));
        originalFile.setMimetype(omero.rtypes.rstring("")); // or "application/octet-stream"
        // now we save the originalFile object
        originalFile = (OriginalFile) iUpdate.saveAndReturnObject(originalFile);

        // Initialize the service to load the raw data
        RawFileStorePrx rawFileStore = entryUnencrypted.createRawFileStore();
        rawFileStore.setFileId(originalFile.getId().getValue());

        FileInputStream stream = new FileInputStream(file);
        long pos = 0;
        int rlen;
        byte[] buf = new byte[(int) ImageId];
        ByteBuffer bbuf;
        while ((rlen = stream.read(buf)) > 0) {
            rawFileStore.write(buf, pos, rlen);
            pos += rlen;
            bbuf = ByteBuffer.wrap(buf);
            bbuf.limit(rlen);
        }
        stream.close();

        originalFile = rawFileStore.save();
        // Important to close the service
        rawFileStore.close();

        //now we have an original File in the database and raw data uploaded.
        // We now need to link the Original file to the image using
        // the File annotation object. This is the way to do it.
        FileAnnotation fa = new FileAnnotationI();
        fa.setFile(originalFile);
        fa.setDescription(omero.rtypes.rstring(""));
        fa.setNs(omero.rtypes.rstring(ICY_NameSpace)); // The name space you have set to identify the file annotation.

        // save the file annotation.
        fa = (FileAnnotation) iUpdate.saveAndReturnObject(fa);

        // now link the image and the annotation
        ImageAnnotationLink link = new ImageAnnotationLinkI();
        link.setChild(fa);
        link.setParent(image.asImage());
        // save the link back to the server.
        link = (ImageAnnotationLink) iUpdate.saveAndReturnObject(link);
        // To attach to a Dataset use DatasetAnnotationLink;
    }

 
    public static void main(String [ ] args){
        String hostName = "octopus.openmicroscopy.org";
        int port = 4064;
        String userName = "user-1";
        String password = "ome";
        client client = omeroLogin(hostName, port, userName, password);
        ServiceFactoryPrx session = client.getSession();
        long groupId;
        try {
            groupId = session.getAdminService().getEventContext().groupId;
            long imageId = 21054;
            openOmeroImage(hostName, String.valueOf(port), userName, password, String.valueOf(groupId), String.valueOf(imageId));
        } catch (ServerError e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        };

        client.closeSession();
    }

    public static Roi convertToOmeroRoi(List<ROI2D> Rois)
    {
        Roi result = new RoiI();
        for (int i=0; i<Rois.size() ; i++){
            ROI2D icyRois = Rois.get(i);
            RInt roiT = omero.rtypes.rint(icyRois.getT());
            RInt roiZ = omero.rtypes.rint(icyRois.getZ());
            Polygon polygon;
            Polyline polyline;
            Ellipse ellipse;
            Rect rect;
            Line line;
            Point point;
            if (icyRois instanceof ROI2DShape)
            {
                final List<Point2D> pts = ((ROI2DShape) icyRois).getPoints();

                if (icyRois instanceof ROI2DPoint)
                {
                    final Point2D p = pts.get(0);
                    point = setOmeroPoint(omero.rtypes.rdouble(p.getX()),omero.rtypes.rdouble(p.getY()),roiT,roiZ);
                    result.addShape(point);
                }
                else if (icyRois instanceof ROI2DLine)
                {
                    final Point2D p1 = pts.get(0);
                    final Point2D p2 = pts.get(1);
                    line = setOmeroLine(omero.rtypes.rdouble(p1.getX()),omero.rtypes.rdouble(p2.getX()),omero.rtypes.rdouble(p1.getY()),omero.rtypes.rdouble(p2.getY()),roiT,roiZ);
                    result.addShape(line);
                }
                else if (icyRois instanceof ROI2DRectangle)
                {
                    final Rectangle2D r = icyRois.getBounds2D();
                    rect = setOmeroRect(omero.rtypes.rdouble(r.getX()),omero.rtypes.rdouble(r.getY()),omero.rtypes.rdouble(r.getWidth()),omero.rtypes.rdouble(r.getHeight()),roiT,roiZ);
                    result.addShape(rect);
                }
                else if (icyRois instanceof ROI2DEllipse)
                {
                    final Rectangle2D r = icyRois.getBounds2D();
                    ellipse = setOmeroEllipse(omero.rtypes.rdouble(r.getX()),omero.rtypes.rdouble(r.getY()),omero.rtypes.rdouble(r.getWidth()),omero.rtypes.rdouble(r.getHeight()),roiT,roiZ);
                    result.addShape(ellipse);
                }
                else if ((icyRois instanceof ROI2DPolyLine) || (icyRois instanceof ROI2DPolygon))
                {
                    String points = null;
                    int cntr = 0;
                    for (Point2D p : pts){
                        if(cntr==0){
                            points = (p.getX() + "," + p.getY());
                        }else{
                            points= (points + " " + p.getX() + "," + p.getY());
                        }
                        cntr ++;
                    }

                    if (icyRois instanceof ROI2DPolyLine){
                        polyline = setOmeroPolyline(omero.rtypes.rstring(points),roiT,roiZ);
                        result.addShape(polyline);
                    }
                    else{
                        polygon = setOmeroPolygon(omero.rtypes.rstring(points),roiT,roiZ);
                        result.addShape(polygon);
                    }
                }
                else{
                    // create compatible shape ROI
                    Shape shaperoi = ((ROI2DShape) icyRois).getShape();
                    if (shaperoi instanceof ROI2DPoint){
                        final Point2D p = pts.get(0);
                        point = setOmeroPoint(omero.rtypes.rdouble(p.getX()),omero.rtypes.rdouble(p.getY()),roiT,roiZ);
                        result.addShape(point);
                    }
                    else if (shaperoi instanceof ROI2DLine)
                    {
                        final Point2D p1 = pts.get(0);
                        final Point2D p2 = pts.get(1);
                        line = setOmeroLine(omero.rtypes.rdouble(p1.getX()),omero.rtypes.rdouble(p2.getX()),omero.rtypes.rdouble(p1.getY()),omero.rtypes.rdouble(p2.getY()),roiT,roiZ);
                        result.addShape(line);
                    }
                    else if (shaperoi instanceof ROI2DRectangle)
                    {
                        final Rectangle2D r = icyRois.getBounds2D();
                        rect = setOmeroRect(omero.rtypes.rdouble(r.getX()),omero.rtypes.rdouble(r.getY()),omero.rtypes.rdouble(r.getWidth()),omero.rtypes.rdouble(r.getHeight()),roiT,roiZ);
                        result.addShape(rect);
                    }
                    else if (shaperoi instanceof ROI2DEllipse)
                    {
                        final Rectangle2D r = icyRois.getBounds2D();
                        ellipse = setOmeroEllipse(omero.rtypes.rdouble(r.getX()),omero.rtypes.rdouble(r.getY()),omero.rtypes.rdouble(r.getWidth()),omero.rtypes.rdouble(r.getHeight()),roiT,roiZ);
                        result.addShape(ellipse);
                    }
                    else if ((shaperoi instanceof ROI2DPolyLine) || (icyRois instanceof ROI2DPolygon))
                    {
                        String points = null;
                        int cntr = 0;
                        for (Point2D p : pts){
                            if(cntr==0){
                                points = (p.getX() + "," + p.getY());
                            }else{
                                points= (points + " " + p.getX() + "," + p.getY());
                            }
                            cntr ++;
                        }

                        if (icyRois instanceof ROI2DPolyLine){
                            polyline = setOmeroPolyline(omero.rtypes.rstring(points),roiT,roiZ);
                            result.addShape(polyline);
                        }
                        else{
                            polygon = setOmeroPolygon(omero.rtypes.rstring(points),roiT,roiZ);
                            result.addShape(polygon);
                        }
                    }
                }

            }
            else if (icyRois instanceof ROI2DArea)
            {
                final ROI2DArea roiArea = (ROI2DArea) icyRois;
                final java.awt.Point[] point2 = roiArea.getBooleanMask(true).getPoints();

                String points = null;
                int cntr = 0;
                for (java.awt.Point pt : point2){

                    if(cntr==0){
                        points = (pt.x + "," + pt.y);
                    }else{
                        points= (points + " " + pt.x + "," + pt.y);
                    }
                    cntr ++;
                }
                polygon = setOmeroPolygon(omero.rtypes.rstring(points),roiT,roiZ);
                result.addShape(polygon);
            }
            else
            {
                // create standard ROI
                final Rectangle2D r = icyRois.getBounds2D();
                rect = setOmeroRect(omero.rtypes.rdouble(r.getX()),omero.rtypes.rdouble(r.getY()),omero.rtypes.rdouble(r.getWidth()),omero.rtypes.rdouble(r.getHeight()),roiT,roiZ);
                result.addShape(rect);
                //        result.setName(roi.getName());
                //        result.setStrokeColor(roi.getColor());
                // result.setFillColor(roi.getColor());
                // result.setStrokeWidth(roi.getStroke());
            }
        }
        System.out.println("Success");
        return result;
    }

    private static Point setOmeroPoint(RDouble Cx, RDouble Cy,
            RInt roiT, RInt roiZ) {
        Point point = new PointI();
        point.setCx(Cx);
        point.setCy(Cy);
        point.setTheT(roiT);
        point.setTheZ(roiZ);
        return point;
    }

    private static Line setOmeroLine(RDouble X1, RDouble Y1,
            RDouble X2, RDouble Y2, RInt roiT, RInt roiZ) {
        Line line = new LineI();
        line.setX1(X1);
        line.setX2(X2);
        line.setY1(Y1);
        line.setY2(Y2);
        line.setTheT(roiT);
        line.setTheZ(roiZ);
        return null;
    }

    private static Rect setOmeroRect(RDouble X, RDouble Y,
            RDouble width, RDouble height, RInt roiT, RInt roiZ) {
        Rect rect = new RectI();
        rect.setX(X);
        rect.setY(Y);
        rect.setWidth(width);
        rect.setHeight(height);
        rect.setTheT(roiT);
        rect.setTheZ(roiZ);
        return null;
    }

    private static Ellipse setOmeroEllipse(RDouble Cx, RDouble Cy,
            RDouble Rx, RDouble Ry, RInt roiT, RInt roiZ) {
        Ellipse ellipse = new EllipseI();
        ellipse.setCx(Cx);
        ellipse.setCy(Cy);
        ellipse.setRx(Rx);
        ellipse.setRy(Ry);
        ellipse.setTheT(roiT);
        ellipse.setTheZ(roiZ);
        return ellipse;
    }

    private static Polyline setOmeroPolyline(RString points, RInt roiT,
            RInt roiZ) {
        Polyline polyline = new PolylineI();
        polyline.setPoints(points);
        polyline.setTheT(roiT);
        polyline.setTheZ(roiZ);
        return polyline;
    }

    private static Polygon setOmeroPolygon(RString points, RInt roiT,
            RInt roiZ) {
        Polygon polygon = new PolygonI();
        polygon.setPoints(points);
        polygon.setTheT(roiT);
        polygon.setTheZ(roiZ);
        return polygon;
    }
}


