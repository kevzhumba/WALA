package com.ibm.wala.core.tests.callGraph;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;

/** The receiver of a virtual call should only flow to the override it dispatches to. */
public class ReceiverDispatchTest extends WalaTestCase {

  @Test
  public void thisOnlyHoldsDispatchingReceivers() throws Exception {
    AnalysisScope scope =
        CallGraphTestUtil.makeJ2SEAnalysisScope(
            TestConstants.WALA_TESTDATA, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Iterable<Entrypoint> entrypoints = Util.makeMainEntrypoints(cha, "Lcpa/ReceiverDispatch");
    AnalysisOptions options = CallGraphTestUtil.makeAnalysisOptions(scope, entrypoints);
    SSAPropagationCallGraphBuilder builder =
        Util.makeZeroCFABuilder(Language.JAVA, options, new AnalysisCacheImpl(), cha);
    CallGraph cg = builder.makeCallGraph(options, null);
    PointerAnalysis<InstanceKey> pa = builder.getPointerAnalysis();

    assertThat(thisTypes(cg, pa, "A")).containsExactly("Lcpa/ReceiverDispatch$A");
    assertThat(thisTypes(cg, pa, "B")).containsExactly("Lcpa/ReceiverDispatch$B");
  }

  private static Set<String> thisTypes(CallGraph cg, PointerAnalysis<InstanceKey> pa, String cls) {
    TypeReference t =
        TypeReference.findOrCreate(
            ClassLoaderReference.Application, "Lcpa/ReceiverDispatch$" + cls);
    MethodReference foo =
        MethodReference.findOrCreate(
            t,
            Atom.findOrCreateUnicodeAtom("foo"),
            Descriptor.findOrCreateUTF8("()Ljava/lang/Object;"));
    Set<CGNode> nodes = cg.getNodes(foo);
    assertThat(nodes).hasSize(1);
    CGNode node = nodes.iterator().next();
    return StreamSupport.stream(
            pa.getPointsToSet(new LocalPointerKey(node, node.getIR().getParameter(0)))
                .spliterator(),
            false)
        .map(ik -> ik.getConcreteType().getName().toString())
        .collect(Collectors.toSet());
  }
}
