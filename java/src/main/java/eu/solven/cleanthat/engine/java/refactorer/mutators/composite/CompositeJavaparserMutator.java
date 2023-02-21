///*
// * Copyright 2023 Benoit Lacelle - SOLVEN
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package eu.solven.cleanthat.engine.java.refactorer.mutators.composite;
//
//import java.util.List;
//
//import com.github.javaparser.ast.Node;
//
//import eu.solven.cleanthat.engine.java.refactorer.meta.IJavaparserMutator;
//
///**
// * A {@link CompositeMutator} dedicated for {@link IJavaparserMutator}
// * 
// * @author Benoit Lacelle
// *
// */
//@Deprecated(since = "Is there a real usage for this?")
//public class CompositeJavaparserMutator extends CompositeMutator<IJavaparserMutator> implements IJavaparserMutator {
//
//	protected CompositeJavaparserMutator(List<IJavaparserMutator> mutators) {
//		super(mutators);
//	}
//
//	@Override
//	public boolean walkNode(Node pre) {
//		boolean modified = false;
//
//		for (IJavaparserMutator mutator : mutators) {
//			modified |= mutator.walkNode(pre);
//		}
//
//		return modified;
//	}
//
//}
