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


import icy.imagej.ImageJUtil;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;
import ij.ImagePlus;
import ij.WindowManager;
import omero.ServerError;
import omero.client;
import omero.api.ServiceFactoryPrx;
import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.*;
import plugins.adufour.vars.lang.VarObject;
import plugins.adufour.vars.lang.VarSequence;
import pojos.ProjectData;


/**
 * 
 *
 * @author Balaji Ramalingam &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:b.ramalingam@dundee.ac.uk">b.ramalingam@dundee.ac.uk</a>
 * @since 5.1
 */
public class OmeroToIcy extends EzPlug implements EzStoppable,Block{

    EzVarText                                           varServer;
    EzVarText                                           varPort;
    EzVarText                                           varUserName;
    EzVarText                                           varPassword;
    EzVarText                                           varImageId;
    VarObject                                           varClient;
    client                                              client;
    ServiceFactoryPrx                                   session;
    Sequence                                            seq;
    VarSequence                                         sequence;
    ProjectData                                         project;
    EzVarSequence                                       image;

    // some other data
    boolean                                             stopFlag;

    @Override
    protected void initialize()
    {
        // 1) variables must be initialized
        varServer = new EzVarText("Server Address:", new String[] { "Server Address" }, 0, true);
        varPort = new EzVarText("Port:", new String[]{"port"}, 0, true);
        varUserName = new EzVarText("UserName:", new String[]{"Username"}, 0, true);
        varPassword = new EzVarText("Password:", new String[]{"Password"}, 0, true);
        varImageId = new EzVarText("ImageId:", new String[]{"ImageID"}, 0, true);
        varClient = new VarObject("Client",client);
        EzGroup groupCredentials = new EzGroup("OMERO Login Credentials", varServer, varPort, varUserName, varPassword, varImageId);

        image = new EzVarSequence("Output sequence");

        super.addEzComponent(groupCredentials);
    }

    VarSequence outputSequence = new VarSequence("output sequence", null);
    @Override
    protected void execute()
    {

        String hostName = varServer.getValue();
        String port = varPort.getValue();
        String userName = varUserName.getValue();
        String password = varPassword.getValue();
        String ImageId = varImageId.getValue();

        client = testOmero.omeroLogin(hostName, Integer.valueOf(port), userName, password);
        session = client.getSession();
        varClient.setValue(client);

        long groupId;

        try {
            groupId = session.getAdminService().getEventContext().groupId;
            testOmero.openOmeroImage(hostName, port, userName, password, String.valueOf(groupId), ImageId);
            ImagePlus ip = WindowManager.getCurrentImage();

            if (ip != null)
            {
                seq = ImageJUtil.convertToIcySequence(ip, null);
                // show the sequence
                addSequence(seq);
                outputSequence.setValue(seq);
                ip.close();
            }
        } catch (ServerError e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        stopFlag = false;
        if(!isHeadLess()){
            super.getUI().setProgressBarMessage("Waiting...");
            int cpt = 0;
            while (!stopFlag)
            {
                cpt++;
                if (cpt % 10 == 0) super.getUI().setProgressBarValue((cpt % 5000000) / 5000000.0);
                Thread.yield();
            }
        }

    }


    @Override
    public void clean()
    {
        // use this method to clean local variables or input streams (if any) to avoid memory leaks
        if (client != null){
            client.closeSession();
        }
        Thread.yield();
    }

    @Override
    public void stopExecution()
    {
        // this method is from the EzStoppable interface
        // if this interface is implemented, a "stop" button is displayed
        // and this method is called when the user hits the "stop" button
        stopFlag = true;
    }

    @Override
    public void declareInput(VarList inputMap) {
        // TODO Auto-generated method stub
        initialize();
        inputMap.add("Server",varServer.getVariable());
        inputMap.add("Port",varPort.getVariable());
        inputMap.add("UserName",varUserName.getVariable());
        inputMap.add("Password",varPassword.getVariable());
        inputMap.add("ImageId",varImageId.getVariable());
    }

    @Override
    public void declareOutput(VarList outputMap) {

        // TODO Auto-generated method stub
        outputMap.add("OMEROClient",varClient);
        outputMap.add("OMEROImage", outputSequence);
    }


}
