/*
 * Copyright (C) 2011-2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file is auto-generated. DO NOT MODIFY!
 * The source Renderscript file: /Users/alankila/Open_Source/live-mandelbox/app/src/main/rs/render.fs
 */

package fi.bel.android.mandelbox.render;

import android.renderscript.*;
import fi.bel.android.mandelbox.render.renderBitCode;

/**
 * @hide
 */
public class ScriptC_render extends ScriptC {
    private static final String __rs_resource_name = "render";
    // Constructor
    public  ScriptC_render(RenderScript rs) {
        super(rs,
              __rs_resource_name,
              renderBitCode.getBitCode32(),
              renderBitCode.getBitCode64());
        mExportVar_seed = 1;
        __I32 = Element.I32(rs);
        mExportVar_scale = 2.f;
        __F32 = Element.F32(rs);
        __U8_4 = Element.U8_4(rs);
    }

    private Element __F32;
    private Element __I32;
    private Element __U8_4;
    private FieldPacker __rs_fp_F32;
    private FieldPacker __rs_fp_I32;
    private final static int mExportVarIdx_seed = 0;
    private int mExportVar_seed;
    public synchronized void set_seed(int v) {
        setVar(mExportVarIdx_seed, v);
        mExportVar_seed = v;
    }

    public int get_seed() {
        return mExportVar_seed;
    }

    public Script.FieldID getFieldID_seed() {
        return createFieldID(mExportVarIdx_seed, null);
    }

    private final static int mExportVarIdx_scale = 1;
    private float mExportVar_scale;
    public synchronized void set_scale(float v) {
        setVar(mExportVarIdx_scale, v);
        mExportVar_scale = v;
    }

    public float get_scale() {
        return mExportVar_scale;
    }

    public Script.FieldID getFieldID_scale() {
        return createFieldID(mExportVarIdx_scale, null);
    }

    private final static int mExportVarIdx_invDim = 2;
    private float mExportVar_invDim;
    public synchronized void set_invDim(float v) {
        setVar(mExportVarIdx_invDim, v);
        mExportVar_invDim = v;
    }

    public float get_invDim() {
        return mExportVar_invDim;
    }

    public Script.FieldID getFieldID_invDim() {
        return createFieldID(mExportVarIdx_invDim, null);
    }

    private final static int mExportForEachIdx_root = 0;
    public Script.KernelID getKernelID_root() {
        return createKernelID(mExportForEachIdx_root, 58, null, null);
    }

    public void forEach_root(Allocation aout) {
        forEach_root(aout, null);
    }

    public void forEach_root(Allocation aout, Script.LaunchOptions sc) {
        // check aout
        if (!aout.getType().getElement().isCompatible(__U8_4)) {
            throw new RSRuntimeException("Type mismatch with U8_4!");
        }
        forEach(mExportForEachIdx_root, (Allocation) null, aout, null, sc);
    }

    private final static int mExportFuncIdx_collect_exposure = 0;
    public void invoke_collect_exposure() {
        invoke(mExportFuncIdx_collect_exposure);
    }

    private final static int mExportFuncIdx_randomize_position = 1;
    public void invoke_randomize_position() {
        invoke(mExportFuncIdx_randomize_position);
    }

    private final static int mExportFuncIdx_adjust_rot = 2;
    public void invoke_adjust_rot(float rot) {
        FieldPacker adjust_rot_fp = new FieldPacker(4);
        adjust_rot_fp.addF32(rot);
        invoke(mExportFuncIdx_adjust_rot, adjust_rot_fp);
    }

}

