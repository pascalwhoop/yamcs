package org.yamcs.web.rest.mdb;

import org.yamcs.protobuf.Mdb.AlgorithmInfo;
import org.yamcs.protobuf.Rest.ListAlgorithmInfoResponse;
import org.yamcs.security.SystemPrivilege;
import org.yamcs.web.HttpException;
import org.yamcs.web.rest.RestHandler;
import org.yamcs.web.rest.RestRequest;
import org.yamcs.web.rest.Route;
import org.yamcs.web.rest.mdb.XtceToGpbAssembler.DetailLevel;
import org.yamcs.xtce.Algorithm;
import org.yamcs.xtce.XtceDb;
import org.yamcs.xtceproc.XtceDbFactory;

/**
 * Handles incoming requests related to algorithm info from the MDB
 */
public class MDBAlgorithmRestHandler extends RestHandler {

    @Route(path = "/api/mdb/:instance/algorithms", method = "GET")
    @Route(path = "/api/mdb/:instance/algorithms/:name*", method = "GET")
    public void getAlgorithm(RestRequest req) throws HttpException {
        checkSystemPrivilege(req, SystemPrivilege.GetMissionDatabase);

        if (req.hasRouteParam("name")) {
            getAlgorithmInfo(req);
        } else {
            listAlgorithms(req);
        }
    }

    private void getAlgorithmInfo(RestRequest req) throws HttpException {
        String instance = verifyInstance(req, req.getRouteParam("instance"));

        XtceDb mdb = XtceDbFactory.getInstance(instance);
        Algorithm algo = verifyAlgorithm(req, mdb, req.getRouteParam("name"));

        AlgorithmInfo cinfo = XtceToGpbAssembler.toAlgorithmInfo(algo, DetailLevel.FULL);
        completeOK(req, cinfo);
    }

    private void listAlgorithms(RestRequest req) throws HttpException {
        String instance = verifyInstance(req, req.getRouteParam("instance"));
        XtceDb mdb = XtceDbFactory.getInstance(instance);

        // Should eventually be replaced in a generic mdb search operation
        NameDescriptionSearchMatcher matcher = null;
        if (req.hasQueryParameter("q")) {
            matcher = new NameDescriptionSearchMatcher(req.getQueryParameter("q"));
        }

        boolean recurse = req.getQueryParameterAsBoolean("recurse", false);

        ListAlgorithmInfoResponse.Builder responseb = ListAlgorithmInfoResponse.newBuilder();
        if (req.hasQueryParameter("namespace")) {
            String namespace = req.getQueryParameter("namespace");

            for (Algorithm algo : mdb.getAlgorithms()) {
                if (matcher != null && !matcher.matches(algo)) {
                    continue;
                }

                String alias = algo.getAlias(namespace);
                if (alias != null || (recurse && algo.getQualifiedName().startsWith(namespace))) {
                    responseb.addAlgorithm(XtceToGpbAssembler.toAlgorithmInfo(algo, DetailLevel.SUMMARY));
                }
            }
        } else { // List all
            for (Algorithm algo : mdb.getAlgorithms()) {
                if (matcher != null && !matcher.matches(algo)) {
                    continue;
                }
                responseb.addAlgorithm(XtceToGpbAssembler.toAlgorithmInfo(algo, DetailLevel.SUMMARY));
            }
        }

        completeOK(req, responseb.build());
    }
}
