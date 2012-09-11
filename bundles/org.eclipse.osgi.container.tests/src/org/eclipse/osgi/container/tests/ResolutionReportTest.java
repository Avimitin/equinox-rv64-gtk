/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.container.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.*;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.tests.dummys.*;
import org.eclipse.osgi.framework.report.ResolutionReport;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Resource;
import org.osgi.service.resolver.ResolutionException;

public class ResolutionReportTest extends AbstractTest {
	@Test
	public void testResolutionReportListenerService() throws Exception {
		DummyResolverHook hook = new DummyResolverHook();
		registerService(org.osgi.framework.hooks.resolver.ResolverHookFactory.class, new DummyResolverHookFactory(hook));
		getSystemBundle().adapt(FrameworkWiring.class).resolveBundles(Collections.singleton(getSystemBundle()));
		assertEquals("No resolution report listener callback", 1, hook.getResolutionReports().size());
		assertNotNull("Resolution report was null", hook.getResolutionReports().get(0));
	}

	@Test
	public void testResolutionReportListenerModule() throws Exception {
		DummyResolverHook hook = new DummyResolverHook();
		DummyContainerAdaptor adaptor = createDummyAdaptor(hook);
		ModuleContainer container = adaptor.getContainer();
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);
		container.resolve(Arrays.asList(systemBundle), true);
		assertEquals("No resolution report listener callback", 1, hook.getResolutionReports().size());
		assertNotNull("Resolution report was null", hook.getResolutionReports().get(0));
	}

	@Test
	public void testResolutionReportBuilder() {
		org.eclipse.osgi.container.ResolutionReport.Builder builder = new org.eclipse.osgi.container.ResolutionReport.Builder();
		ResolutionReport report = builder.build();
		assertNotNull("Resolution report was null", report);
	}

	@Test
	public void testFilteredByResolverHook() throws Exception {
		DummyResolverHook hook = new DummyResolverHook() {
			@Override
			public void filterResolvable(Collection<BundleRevision> candidates) {
				candidates.clear();
			}
		};
		DummyContainerAdaptor adaptor = createDummyAdaptor(hook);
		ModuleContainer container = adaptor.getContainer();
		Module module = installDummyModule("resolution.report.a.MF", "resolution.report.a", container);
		assertResolutionDoesNotSucceed(container, Arrays.asList(module));
		ResolutionReport report = hook.getResolutionReports().get(0);
		Map<Resource, List<ResolutionReport.Entry>> resourceToEntries = report.getEntries();
		assertResolutionReportEntriesSize(resourceToEntries, 1);
		List<ResolutionReport.Entry> entries = resourceToEntries.get(module.getCurrentRevision());
		assertResolutionReportEntriesSize(entries, 1);
		ResolutionReport.Entry entry = entries.get(0);
		assertResolutionReportEntryTypeFilteredByResolverHook(entry.getType());
		assertResolutionReportEntryDataNull(entry.getData());
	}

	@Test
	public void testFilteredBySingletonNoneResolved() throws Exception {
		DummyResolverHook hook = new DummyResolverHook();
		DummyContainerAdaptor adaptor = createDummyAdaptor(hook);
		ModuleContainer container = adaptor.getContainer();
		Module resolutionReporta = installDummyModule("resolution.report.a.MF", "resolution.report.a", container);
		Module resolutionReportaV1 = installDummyModule("resolution.report.a.v1.MF", "resolution.report.a.v1", container);
		assertResolutionDoesNotSucceed(container, Arrays.asList(resolutionReporta, resolutionReportaV1));
		ResolutionReport report = hook.getResolutionReports().get(0);
		Map<Resource, List<ResolutionReport.Entry>> resourceToEntries = report.getEntries();
		assertResolutionReportEntriesSize(resourceToEntries, 1);
		List<ResolutionReport.Entry> entries = resourceToEntries.get(resolutionReporta.getCurrentRevision());
		assertResolutionReportEntriesSize(entries, 1);
		ResolutionReport.Entry entry = entries.get(0);
		assertResolutionReportEntryTypeSingletonSelection(entry.getType());
		assertResolutionReportEntryDataNotNull(entry.getData());
	}

	@Test
	public void testFilteredBySingletonHighestVersionResolved() throws Exception {
		DummyResolverHook hook = new DummyResolverHook();
		DummyContainerAdaptor adaptor = createDummyAdaptor(hook);
		ModuleContainer container = adaptor.getContainer();
		Module resolutionReporta = installDummyModule("resolution.report.a.MF", "resolution.report.a", container);
		Module resolutionReportaV1 = installDummyModule("resolution.report.a.v1.MF", "resolution.report.a.v1", container);
		container.resolve(Arrays.asList(resolutionReportaV1), true);
		clearResolutionReports(hook);
		assertResolutionDoesNotSucceed(container, Arrays.asList(resolutionReporta));
		ResolutionReport report = hook.getResolutionReports().get(0);
		Map<Resource, List<ResolutionReport.Entry>> resourceToEntries = report.getEntries();
		assertResolutionReportEntriesSize(resourceToEntries, 1);
		List<ResolutionReport.Entry> entries = resourceToEntries.get(resolutionReporta.getCurrentRevision());
		assertResolutionReportEntriesSize(entries, 1);
		ResolutionReport.Entry entry = entries.get(0);
		assertResolutionReportEntryTypeSingletonSelection(entry.getType());
		assertResolutionReportEntryDataNotNull(entry.getData());
	}

	@Test
	public void testFilteredBySingletonLowestVersionResolved() throws Exception {
		DummyResolverHook hook = new DummyResolverHook();
		DummyContainerAdaptor adaptor = createDummyAdaptor(hook);
		ModuleContainer container = adaptor.getContainer();
		Module resolutionReporta = installDummyModule("resolution.report.a.MF", "resolution.report.a", container);
		container.resolve(Arrays.asList(resolutionReporta), true);
		clearResolutionReports(hook);
		Module resolutionReportaV1 = installDummyModule("resolution.report.a.v1.MF", "resolution.report.a.v1", container);
		assertResolutionDoesNotSucceed(container, Arrays.asList(resolutionReportaV1));
		ResolutionReport report = hook.getResolutionReports().get(0);
		Map<Resource, List<ResolutionReport.Entry>> resourceToEntries = report.getEntries();
		assertResolutionReportEntriesSize(resourceToEntries, 1);
		List<ResolutionReport.Entry> entries = resourceToEntries.get(resolutionReportaV1.getCurrentRevision());
		assertResolutionReportEntriesSize(entries, 1);
		ResolutionReport.Entry entry = entries.get(0);
		assertResolutionReportEntryTypeSingletonSelection(entry.getType());
		assertResolutionReportEntryDataNotNull(entry.getData());
	}

	private void clearResolutionReports(DummyResolverHook hook) {
		hook.getResolutionReports().clear();
	}

	private void assertResolutionDoesNotSucceed(ModuleContainer container, Collection<Module> modules) {
		try {
			container.resolve(modules, true);
			fail("Resolution should not have succeeded");
		} catch (ResolutionException e) {
			// Okay.
		}
	}

	private void assertResolutionReportEntriesNotNull(Map<Resource, List<ResolutionReport.Entry>> entries) {
		assertNotNull("Resolution report entries was null", entries);
	}

	private void assertResolutionReportEntriesSize(Map<Resource, List<ResolutionReport.Entry>> entries, int expected) {
		assertResolutionReportEntriesNotNull(entries);
		assertEquals("Wrong number of total resolution report entries", expected, entries.size());
	}

	private void assertResolutionReportEntriesNotNull(List<ResolutionReport.Entry> entries) {
		assertNotNull("Resolution report entries for resource was null", entries);
	}

	private void assertResolutionReportEntriesSize(List<ResolutionReport.Entry> entries, int expected) {
		assertResolutionReportEntriesNotNull(entries);
		assertEquals("Wrong number of resolution report entries", expected, entries.size());
	}

	private void assertResolutionReportEntryDataNotNull(Object data) {
		assertNotNull("No resolution report entry data", data);
	}

	private void assertResolutionReportEntryDataNull(Object data) {
		assertEquals("Unexpected resolution report entry data", null, data);
	}

	private void assertResolutionReportEntryTypeFilteredByResolverHook(ResolutionReport.Entry.Type type) {
		assertResolutionReportEntryType(ResolutionReport.Entry.Type.FILTERED_BY_RESOLVER_HOOK, type);
	}

	private void assertResolutionReportEntryTypeSingletonSelection(ResolutionReport.Entry.Type type) {
		assertResolutionReportEntryType(ResolutionReport.Entry.Type.SINGLETON_SELECTION, type);
	}

	private void assertResolutionReportEntryType(ResolutionReport.Entry.Type expected, ResolutionReport.Entry.Type actual) {
		assertEquals("Wrong resolution report entry type", expected, actual);
	}
}
